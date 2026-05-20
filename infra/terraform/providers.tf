provider "digitalocean" {}

provider "cloudflare" {
  api_token = var.cloudflare_api_token
}

provider "kubernetes" {
  host  = digitalocean_kubernetes_cluster.tranquilai.endpoint
  token = digitalocean_kubernetes_cluster.tranquilai.kube_config[0].token

  cluster_ca_certificate = base64decode(
    digitalocean_kubernetes_cluster.tranquilai.kube_config[0].cluster_ca_certificate
  )
}

provider "helm" {
  kubernetes {
    host  = digitalocean_kubernetes_cluster.tranquilai.endpoint
    token = digitalocean_kubernetes_cluster.tranquilai.kube_config[0].token

    cluster_ca_certificate = base64decode(
      digitalocean_kubernetes_cluster.tranquilai.kube_config[0].cluster_ca_certificate
    )
  }
}
