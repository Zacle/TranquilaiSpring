param(
  [string]$BaseUrl = "https://api.tranquilai.cloud",
  [int]$TimeoutSeconds = 30
)

$ErrorActionPreference = "Stop"
$HealthUrl = "$BaseUrl/actuator/health"

$Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$LastFailure = "No request attempted."
do {
  try {
    $Response = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 10
    if ($Response.status -eq "UP") {
      Write-Host "Gateway health is UP at $HealthUrl"
      exit 0
    }
    $LastFailure = "Health endpoint returned status '$($Response.status)' with body: $($Response | ConvertTo-Json -Compress)"
  } catch {
    $LastFailure = $_.Exception.Message
    if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
      $LastFailure = "$LastFailure Body: $($_.ErrorDetails.Message)"
    }
    Start-Sleep -Seconds 2
  }
} while ((Get-Date) -lt $Deadline)

throw "Smoke test failed: gateway health did not return UP at $HealthUrl within $TimeoutSeconds seconds. Last failure: $LastFailure"
