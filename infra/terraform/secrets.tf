resource "kubernetes_secret_v1" "cloudflare_api_token" {
  depends_on = [helm_release.cert_manager]

  metadata {
    name      = "cloudflare-api-token-secret"
    namespace = local.cert_manager_ns
  }

  data = {
    "api-token" = var.cloudflare_api_token
  }
}
