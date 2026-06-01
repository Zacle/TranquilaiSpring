output "cluster_name" {
  description = "Ephemeral staging DOKS cluster name."
  value       = digitalocean_kubernetes_cluster.staging.name
}

output "cluster_id" {
  description = "Ephemeral staging DOKS cluster ID."
  value       = digitalocean_kubernetes_cluster.staging.id
}

output "cluster_endpoint" {
  description = "Ephemeral staging DOKS API endpoint."
  value       = digitalocean_kubernetes_cluster.staging.endpoint
  sensitive   = true
}
