output "cluster_name" {
  description = "DOKS cluster name."
  value       = digitalocean_kubernetes_cluster.tranquilai.name
}

output "digitalocean_project_name" {
  description = "DigitalOcean Project name."
  value       = digitalocean_project.tranquilai.name
}

output "cluster_endpoint" {
  description = "DOKS API endpoint."
  value       = digitalocean_kubernetes_cluster.tranquilai.endpoint
  sensitive   = true
}

output "production_namespace" {
  description = "Production namespace."
  value       = kubernetes_namespace.prod.metadata[0].name
}

output "ingress_ip" {
  description = "DigitalOcean load balancer IP assigned to ingress-nginx."
  value       = local.ingress_ip
}
