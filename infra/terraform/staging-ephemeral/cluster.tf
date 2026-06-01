locals {
  sanitized_suffix = substr(replace(lower(var.cluster_name_suffix), "/[^a-z0-9-]/", "-"), 0, 20)
  cluster_name     = "${var.project_name}-staging-${local.sanitized_suffix}"
}

data "digitalocean_kubernetes_versions" "staging" {}

resource "digitalocean_kubernetes_cluster" "staging" {
  name    = local.cluster_name
  region  = var.region
  version = coalesce(var.kubernetes_version, data.digitalocean_kubernetes_versions.staging.latest_version)

  auto_upgrade  = false
  surge_upgrade = true

  node_pool {
    name       = "staging"
    size       = var.node_size
    auto_scale = false
    min_nodes  = var.min_nodes
    max_nodes  = var.max_nodes
    tags       = [var.project_name, "staging", "ephemeral"]
  }
}
