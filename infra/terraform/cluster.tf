resource "digitalocean_kubernetes_cluster" "tranquilai" {
  name    = "${var.project_name}-doks"
  region  = var.region
  version = var.kubernetes_version

  auto_upgrade  = true
  surge_upgrade = true

  node_pool {
    name       = "default"
    size       = var.node_size
    auto_scale = true
    min_nodes  = var.min_nodes
    max_nodes  = var.max_nodes
    tags       = [var.project_name]
  }
}

resource "kubernetes_namespace" "prod" {
  metadata {
    name = local.prod_namespace
  }
}

resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = local.monitoring_ns
  }
}
