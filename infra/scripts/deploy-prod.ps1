param(
  [Parameter(Mandatory = $true)]
  [string]$ImageTag,
  [string]$Namespace = "tranquilai-prod",
  [string]$ReleaseName = "tranquilai"
)

$ErrorActionPreference = "Stop"

foreach ($Command in @("kubectl", "helm", "sops")) {
  if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
    throw "Required command '$Command' was not found on PATH."
  }
}

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$AppsChart = Join-Path $Root "infra\helm\tranquilai-apps"
$ServiceChart = Join-Path $Root "infra\helm\tranquilai-service"
$AppsChartDependencies = Join-Path $AppsChart "charts"
$SecretFile = Join-Path $Root "infra\k8s\secrets\tranquilai-prod.enc.yaml"
$DecryptedSecretFile = Join-Path $env:TEMP "tranquilai-prod-secret.yaml"

if (-not (Test-Path -LiteralPath $SecretFile)) {
  throw "Missing production SOPS secret file: $SecretFile."
}

kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -
sops --decrypt --output $DecryptedSecretFile $SecretFile
kubectl apply -n $Namespace -f $DecryptedSecretFile
Remove-Item -LiteralPath $DecryptedSecretFile -Force

New-Item -ItemType Directory -Force $AppsChartDependencies | Out-Null
helm package $ServiceChart --destination $AppsChartDependencies

helm upgrade --install $ReleaseName $AppsChart `
  --namespace $Namespace `
  --set-string api-gateway.image.tag=$ImageTag `
  --set-string auth-service.image.tag=$ImageTag `
  --set-string user-service.image.tag=$ImageTag `
  --set-string ai-service.image.tag=$ImageTag `
  --set-string content-service.image.tag=$ImageTag `
  --set-string activity-service.image.tag=$ImageTag `
  --set-string plan-service.image.tag=$ImageTag `
  --set-string progress-service.image.tag=$ImageTag `
  --set-string notification-service.image.tag=$ImageTag `
  --set-string subscription-service.image.tag=$ImageTag `
  --wait --timeout 10m

kubectl rollout status deployment/tranquilai-api-gateway -n $Namespace --timeout=5m
