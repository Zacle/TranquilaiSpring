param(
  [Parameter(Mandatory = $true)]
  [ValidateSet("staging", "prod", "local")]
  [string]$Environment,

  [Parameter(Mandatory = $true)]
  [string]$Namespace,

  [string]$SecretName = "tranquilai-app-secrets"
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $true
}

foreach ($Command in @("kubectl")) {
  if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
    throw "Required command '$Command' was not found on PATH."
  }
}

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$SqlEnvironment = if ($Environment -eq "local") { "staging" } else { $Environment }
$SqlFile = Join-Path $Root "infra\db\content\urls-$SqlEnvironment.sql"

if (-not (Test-Path -LiteralPath $SqlFile)) {
  throw "Missing content media URL SQL file: $SqlFile"
}

$Suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$ConfigMapName = "content-media-urls-$Suffix"
$JobName = "content-media-urls-$Suffix"

kubectl create configmap $ConfigMapName `
  -n $Namespace `
  --from-file=media-urls.sql=$SqlFile `
  --dry-run=client `
  -o yaml | kubectl apply -f -

$JobManifest = @"
apiVersion: batch/v1
kind: Job
metadata:
  name: $JobName
  namespace: $Namespace
  labels:
    app.kubernetes.io/name: content-media-urls
spec:
  backoffLimit: 1
  ttlSecondsAfterFinished: 300
  template:
    metadata:
      labels:
        app.kubernetes.io/name: content-media-urls
    spec:
      restartPolicy: Never
      containers:
        - name: psql
          image: postgres:17-alpine
          imagePullPolicy: IfNotPresent
          env:
            - name: CONTENT_SERVICE_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: $SecretName
                  key: CONTENT_SERVICE_DATASOURCE_URL
            - name: PGUSER
              valueFrom:
                secretKeyRef:
                  name: $SecretName
                  key: SPRING_DATASOURCE_USERNAME
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: $SecretName
                  key: SPRING_DATASOURCE_PASSWORD
          command:
            - /bin/sh
            - -ec
            - |
              database_url="`$CONTENT_SERVICE_DATASOURCE_URL"
              database_url="`${database_url#jdbc:}"
              psql "`$database_url" -v ON_ERROR_STOP=1 -f /sql/media-urls.sql
          volumeMounts:
            - name: media-urls
              mountPath: /sql
              readOnly: true
      volumes:
        - name: media-urls
          configMap:
            name: $ConfigMapName
"@

$TempManifest = Join-Path ([System.IO.Path]::GetTempPath()) "$JobName.yaml"
$JobManifest | Set-Content -LiteralPath $TempManifest

try {
  kubectl apply -f $TempManifest
  kubectl wait --for=condition=complete "job/$JobName" -n $Namespace --timeout=5m
} catch {
  kubectl logs "job/$JobName" -n $Namespace --tail=200
  throw
} finally {
  Remove-Item -LiteralPath $TempManifest -Force -ErrorAction SilentlyContinue
  kubectl delete configmap $ConfigMapName -n $Namespace --ignore-not-found=true | Out-Null
}
