data "kubernetes_service" "ingress_nginx" {
  depends_on = [helm_release.ingress_nginx]

  metadata {
    name      = "ingress-nginx-controller"
    namespace = local.ingress_ns
  }
}

locals {
  ingress_ip = try(
    data.kubernetes_service.ingress_nginx.status[0].load_balancer[0].ingress[0].ip,
    null
  )
}

resource "cloudflare_record" "api_prod" {
  zone_id = var.cloudflare_zone_id
  name    = var.production_api_hostname
  type    = "A"
  content = local.ingress_ip
  proxied = true

  lifecycle {
    precondition {
      condition     = local.ingress_ip != null
      error_message = "ingress-nginx load balancer IP is not available yet. Re-run terraform apply after DigitalOcean assigns it."
    }
  }
}

resource "cloudflare_record" "api_staging" {
  zone_id = var.cloudflare_zone_id
  name    = var.staging_api_hostname
  type    = "A"
  content = local.ingress_ip
  proxied = true

  lifecycle {
    precondition {
      condition     = local.ingress_ip != null
      error_message = "ingress-nginx load balancer IP is not available yet. Re-run terraform apply after DigitalOcean assigns it."
    }
  }
}

resource "cloudflare_record" "landing" {
  zone_id = var.cloudflare_zone_id
  name    = var.landing_hostname
  type    = "A"
  content = local.ingress_ip
  proxied = true

  lifecycle {
    precondition {
      condition     = local.ingress_ip != null
      error_message = "ingress-nginx load balancer IP is not available yet. Re-run terraform apply after DigitalOcean assigns it."
    }
  }
}

