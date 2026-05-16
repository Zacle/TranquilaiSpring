locals {
  prod_namespace    = "${var.project_name}-prod"
  monitoring_ns     = "monitoring"
  ingress_ns        = "ingress-nginx"
  cert_manager_ns   = "cert-manager"

  root_dir = abspath("${path.module}/../..")
}
