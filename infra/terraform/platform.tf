resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  namespace        = local.ingress_ns
  create_namespace = true

  values = [
    file("${local.root_dir}/infra/k8s/platform/ingress-nginx-values.yaml")
  ]

  wait    = true
  timeout = 600
}

resource "helm_release" "cert_manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  namespace        = local.cert_manager_ns
  create_namespace = true

  set {
    name  = "crds.enabled"
    value = "true"
  }

  wait    = true
  timeout = 600
}

resource "helm_release" "monitoring" {
  count = var.install_monitoring ? 1 : 0

  depends_on = [kubernetes_secret_v1.grafana_admin]

  name       = "kube-prometheus-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [
    file("${local.root_dir}/infra/k8s/platform/kube-prometheus-stack-values.yaml")
  ]

  wait    = true
  timeout = 900
}

resource "helm_release" "rabbitmq_prod" {
  count = var.install_rabbitmq ? 1 : 0

  depends_on = [kubernetes_secret_v1.rabbitmq_prod_auth]

  name       = "rabbitmq"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "rabbitmq"
  namespace  = kubernetes_namespace.prod.metadata[0].name

  values = [
    file("${local.root_dir}/infra/k8s/platform/rabbitmq-values-prod.yaml")
  ]

  wait    = true
  timeout = 600
}
