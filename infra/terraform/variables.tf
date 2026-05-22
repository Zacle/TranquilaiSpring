variable "project_name" {
  description = "Project name used for resource names."
  type        = string
  default     = "tranquilai"
}

variable "digitalocean_project_name" {
  description = "DigitalOcean Project name used to group TranquilAI cloud resources in the DO dashboard."
  type        = string
  default     = "TranquilAI"
}

variable "digitalocean_project_description" {
  description = "DigitalOcean Project description."
  type        = string
  default     = "Production infrastructure for the TranquilAI backend."
}

variable "digitalocean_project_purpose" {
  description = "DigitalOcean Project purpose."
  type        = string
  default     = "Web Application"
}

variable "digitalocean_project_environment" {
  description = "DigitalOcean Project environment."
  type        = string
  default     = "Production"
}

variable "region" {
  description = "DigitalOcean region for the DOKS cluster."
  type        = string
  default     = "nyc3"
}

variable "kubernetes_version" {
  description = "DOKS Kubernetes version slug. Leave null to use the DigitalOcean default."
  type        = string
  default     = null
}

variable "node_size" {
  description = "DigitalOcean droplet size for the default node pool."
  type        = string
  default     = "s-2vcpu-4gb"
}

variable "min_nodes" {
  description = "Minimum nodes in the autoscaling node pool."
  type        = number
  default     = 2
}

variable "max_nodes" {
  description = "Maximum nodes in the autoscaling node pool."
  type        = number
  default     = 4
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID for tranquilai.cloud."
  type        = string
}

variable "production_api_hostname" {
  description = "Production API hostname."
  type        = string
  default     = "api.tranquilai.cloud"
}

variable "landing_hostname" {
  description = "Landing page hostname."
  type        = string
  default     = "tranquilai.cloud"
}

variable "staging_api_hostname" {
  description = "Staging API hostname."
  type        = string
  default     = "api-staging.tranquilai.cloud"
}

variable "cert_manager_email" {
  description = "Email address used for Let's Encrypt ACME registration."
  type        = string
  default     = "platform@tranquilai.cloud"
}

variable "install_monitoring" {
  description = "Whether to install kube-prometheus-stack."
  type        = bool
  default     = true
}

variable "cloudflare_api_token" {
  description = "Cloudflare API token copied into Kubernetes for cert-manager DNS-01 challenges."
  type        = string
  sensitive   = true
}

variable "grafana_admin_user" {
  description = "Grafana admin username."
  type        = string
  default     = "admin"
}

variable "grafana_admin_password" {
  description = "Grafana admin password."
  type        = string
  sensitive   = true
}
