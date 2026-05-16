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

resource "kubernetes_secret_v1" "grafana_admin" {
  metadata {
    name      = "grafana-admin"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }

  data = {
    "admin-user"     = var.grafana_admin_user
    "admin-password" = var.grafana_admin_password
  }
}

resource "kubernetes_secret_v1" "rabbitmq_prod_auth" {
  metadata {
    name      = "rabbitmq-auth"
    namespace = kubernetes_namespace.prod.metadata[0].name
  }

  data = {
    "rabbitmq-password"      = var.rabbitmq_prod_password
    "rabbitmq-erlang-cookie" = var.rabbitmq_prod_erlang_cookie
  }
}
