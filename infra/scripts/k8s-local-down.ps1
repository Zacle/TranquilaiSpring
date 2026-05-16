param(
  [string]$ClusterName = "tranquilai"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command kind -ErrorAction SilentlyContinue)) {
  throw "Required command 'kind' was not found on PATH."
}

kind delete cluster --name $ClusterName
