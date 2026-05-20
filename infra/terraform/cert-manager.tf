resource "helm_release" "letsencrypt_prod" {
  depends_on = [kubernetes_secret_v1.cloudflare_api_token]

  name      = "letsencrypt-prod"
  chart     = "${local.root_dir}/infra/helm/cert-manager-issuer"
  namespace = local.cert_manager_ns

  set {
    name  = "email"
    value = var.cert_manager_email
  }

  wait    = true
  timeout = 300
}
