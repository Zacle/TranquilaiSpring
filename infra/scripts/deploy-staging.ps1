param(
  [Parameter(Mandatory = $true)]
  [string]$ImageTag,
  [string]$Namespace = "tranquilai-staging",
  [string]$ReleaseName = "tranquilai-staging"
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
$StagingValues = Join-Path $AppsChart "values-staging.yaml"
$SecretFile = Join-Path $Root "infra\k8s\secrets\tranquilai-staging.enc.yaml"
$TempDir = [System.IO.Path]::GetTempPath()
$DecryptedSecretFile = Join-Path $TempDir "tranquilai-staging-secret.yaml"

if (-not (Test-Path -LiteralPath $SecretFile)) {
  throw "Missing staging SOPS secret file: $SecretFile. Create it from infra\k8s\secrets\tranquilai-staging.example.yaml."
}

kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -
sops --decrypt --output $DecryptedSecretFile $SecretFile
kubectl apply -n $Namespace -f $DecryptedSecretFile
Remove-Item -LiteralPath $DecryptedSecretFile -Force

New-Item -ItemType Directory -Force $AppsChartDependencies | Out-Null
helm package $ServiceChart --destination $AppsChartDependencies

helm upgrade --install $ReleaseName $AppsChart `
  --namespace $Namespace `
  --values $StagingValues `
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

kubectl rollout status deployment/tranquilai-staging-api-gateway -n $Namespace --timeout=5m
