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

  values = [
    file("${local.root_dir}/infra/k8s/platform/cert-manager-values.yaml")
  ]

  set {
    name  = "crds.enabled"
    value = "true"
  }

  wait    = true
  timeout = 600
}
