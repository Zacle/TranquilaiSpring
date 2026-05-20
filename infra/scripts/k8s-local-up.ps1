param(
  [string]$ClusterName = "tranquilai",
  [string]$Namespace = "tranquilai-staging",
  [string]$ImageTag = "local"
)

$ErrorActionPreference = "Stop"
$Services = @(
  "api-gateway",
  "auth-service",
  "user-service",
  "ai-service",
  "content-service",
  "activity-service",
  "plan-service",
  "progress-service",
  "notification-service",
  "subscription-service"
)

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command '$Name' was not found on PATH."
  }
}

function Invoke-NativeCommand($Command, $Arguments) {
  & $Command @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code ${LASTEXITCODE}: $Command $($Arguments -join ' ')"
  }
}

function Read-SecretStringDataValue($Path, $Key) {
  $Pattern = "^\s*$([regex]::Escape($Key)):\s*(.+?)\s*$"
  foreach ($Line in Get-Content -LiteralPath $Path) {
    if ($Line -match $Pattern) {
      return $Matches[1].Trim().Trim('"').Trim("'")
    }
  }
  throw "Missing required key '$Key' in decrypted secret file."
}

Require-Command kind
Require-Command kubectl
Require-Command helm
Require-Command docker
Require-Command sops

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$KindConfig = Join-Path $Root "infra\kind\cluster.yaml"
$AppsChart = Join-Path $Root "infra\helm\tranquilai-apps"
$ServiceChart = Join-Path $Root "infra\helm\tranquilai-service"
$AppsChartDependencies = Join-Path $AppsChart "charts"
$SecretFile = Join-Path $Root "infra\k8s\secrets\tranquilai-staging.enc.yaml"
$LocalRabbitMqManifest = Join-Path $Root "infra\k8s\platform\rabbitmq-local.yaml"
$DefaultAgeKeyFile = Join-Path $env:USERPROFILE ".config\sops\age\keys.txt"

if (-not $env:SOPS_AGE_KEY_FILE -and (Test-Path $DefaultAgeKeyFile)) {
  $env:SOPS_AGE_KEY_FILE = $DefaultAgeKeyFile
}

if (-not (Test-Path -LiteralPath $SecretFile)) {
  throw "Missing staging SOPS secret file: $SecretFile. Create it from infra\k8s\secrets\tranquilai-staging.example.yaml before running local Kubernetes."
}

if (-not (kind get clusters | Select-String -SimpleMatch $ClusterName)) {
  kind create cluster --name $ClusterName --config $KindConfig
}

kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -

foreach ($Service in $Services) {
  $Dockerfile = Join-Path $Root "$Service\Dockerfile"
  if (Test-Path $Dockerfile) {
    $Image = "ghcr.io/zacle/tranquilai-${Service}:$ImageTag"
    Invoke-NativeCommand docker @("build", "-t", $Image, "-f", $Dockerfile, $Root)
    Invoke-NativeCommand kind @("load", "docker-image", $Image, "--name", $ClusterName)
  } else {
    Write-Warning "Skipping $Service image build because $Dockerfile does not exist."
  }
}

helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx `
  --namespace ingress-nginx --create-namespace `
  --set controller.nodeSelector.ingress-ready=true `
  --set controller.tolerations[0].key=node-role.kubernetes.io/control-plane `
  --set controller.tolerations[0].operator=Exists `
  --set controller.tolerations[0].effect=NoSchedule `
  --set controller.hostPort.enabled=true `
  --set controller.service.type=NodePort `
  --wait

$DecryptedSecretFile = Join-Path $env:TEMP "tranquilai-staging-secret.yaml"
Invoke-NativeCommand sops @("--decrypt", "--output", $DecryptedSecretFile, $SecretFile)
Invoke-NativeCommand kubectl @("apply", "-n", $Namespace, "-f", $DecryptedSecretFile)

$RabbitMqUsername = Read-SecretStringDataValue $DecryptedSecretFile "SPRING_RABBITMQ_USERNAME"
$RabbitMqPassword = Read-SecretStringDataValue $DecryptedSecretFile "SPRING_RABBITMQ_PASSWORD"
$RabbitMqErlangCookie = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
Invoke-NativeCommand kubectl @(
  "create", "secret", "generic", "rabbitmq-auth",
  "-n", $Namespace,
  "--from-literal=rabbitmq-username=$RabbitMqUsername",
  "--from-literal=rabbitmq-password=$RabbitMqPassword",
  "--from-literal=rabbitmq-erlang-cookie=$RabbitMqErlangCookie",
  "--dry-run=client",
  "-o", "yaml"
) | kubectl apply -f -

$LocalRabbitMqPatchFile = Join-Path $env:TEMP "tranquilai-local-rabbitmq-secret-patch.json"
$LocalRabbitMqPatch = @{
  stringData = @{
    SPRING_RABBITMQ_HOST = "rabbitmq.$Namespace.svc.cluster.local"
    SPRING_RABBITMQ_PORT = "5672"
    SPRING_RABBITMQ_VIRTUAL_HOST = "/"
    SPRING_RABBITMQ_SSL_ENABLED = "false"
  }
}
$LocalRabbitMqPatch | ConvertTo-Json -Depth 4 -Compress | Set-Content -LiteralPath $LocalRabbitMqPatchFile
Invoke-NativeCommand kubectl @(
  "patch", "secret", "tranquilai-app-secrets",
  "-n", $Namespace,
  "--type=merge",
  "--patch-file",
  $LocalRabbitMqPatchFile
)
Remove-Item -LiteralPath $LocalRabbitMqPatchFile -Force

Remove-Item -LiteralPath $DecryptedSecretFile -Force

$RabbitMqImage = "tranquilai-rabbitmq-local:4-management-alpine"
$RabbitMqDockerfile = Join-Path $env:TEMP "tranquilai-rabbitmq-local.Dockerfile"
Set-Content -LiteralPath $RabbitMqDockerfile -Value "FROM rabbitmq:4-management-alpine"
Invoke-NativeCommand docker @("build", "--platform", "linux/amd64", "-t", $RabbitMqImage, "-f", $RabbitMqDockerfile, $env:TEMP)
Invoke-NativeCommand kind @("load", "docker-image", $RabbitMqImage, "--name", $ClusterName)
Invoke-NativeCommand kubectl @("apply", "-n", $Namespace, "-f", $LocalRabbitMqManifest)
Invoke-NativeCommand kubectl @("rollout", "status", "deployment/rabbitmq", "-n", $Namespace, "--timeout=5m")

New-Item -ItemType Directory -Force $AppsChartDependencies | Out-Null
helm package $ServiceChart --destination $AppsChartDependencies

helm upgrade --install tranquilai $AppsChart `
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
  --set api-gateway.autoscaling.enabled=false `
  --set auth-service.autoscaling.enabled=false `
  --set user-service.autoscaling.enabled=false `
  --set ai-service.autoscaling.enabled=false `
  --set content-service.autoscaling.enabled=false `
  --set activity-service.autoscaling.enabled=false `
  --set plan-service.autoscaling.enabled=false `
  --set progress-service.autoscaling.enabled=false `
  --set notification-service.autoscaling.enabled=false `
  --set subscription-service.autoscaling.enabled=false `
  --set api-gateway.replicaCount=1 `
  --set auth-service.replicaCount=1 `
  --set user-service.replicaCount=1 `
  --set ai-service.replicaCount=1 `
  --set content-service.replicaCount=1 `
  --set activity-service.replicaCount=1 `
  --set plan-service.replicaCount=1 `
  --set progress-service.replicaCount=1 `
  --set notification-service.replicaCount=1 `
  --set subscription-service.replicaCount=1 `
  --set api-gateway.serviceMonitor.enabled=false `
  --set auth-service.serviceMonitor.enabled=false `
  --set user-service.serviceMonitor.enabled=false `
  --set ai-service.serviceMonitor.enabled=false `
  --set content-service.serviceMonitor.enabled=false `
  --set activity-service.serviceMonitor.enabled=false `
  --set plan-service.serviceMonitor.enabled=false `
  --set progress-service.serviceMonitor.enabled=false `
  --set notification-service.serviceMonitor.enabled=false `
  --set subscription-service.serviceMonitor.enabled=false `
  --set global.imagePullSecrets=null `
  --wait

kubectl get pods -n $Namespace
