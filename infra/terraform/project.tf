resource "digitalocean_project" "tranquilai" {
  name        = var.digitalocean_project_name
  description = var.digitalocean_project_description
  purpose     = var.digitalocean_project_purpose
  environment = var.digitalocean_project_environment
}

resource "digitalocean_project_resources" "tranquilai" {
  project = digitalocean_project.tranquilai.id

  resources = [
    digitalocean_kubernetes_cluster.tranquilai.urn
  ]
}
