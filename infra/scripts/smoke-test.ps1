param(
  [string]$BaseUrl = "https://api.tranquilai.cloud",
  [int]$TimeoutSeconds = 30
)

$ErrorActionPreference = "Stop"
$HealthUrl = "$BaseUrl/actuator/health"

$Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
  try {
    $Response = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 5
    if ($Response.status -eq "UP") {
      Write-Host "Gateway health is UP at $HealthUrl"
      exit 0
    }
  } catch {
    Start-Sleep -Seconds 2
  }
} while ((Get-Date) -lt $Deadline)

throw "Smoke test failed: gateway health did not return UP at $HealthUrl"
