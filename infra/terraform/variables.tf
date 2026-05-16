variable "project_name" {
  description = "Project name used for resource names."
  type        = string
  default     = "tranquilai"
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

variable "install_rabbitmq" {
  description = "Whether to install RabbitMQ Helm releases."
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

variable "rabbitmq_prod_password" {
  description = "RabbitMQ production user password."
  type        = string
  sensitive   = true
}

variable "rabbitmq_prod_erlang_cookie" {
  description = "RabbitMQ production Erlang cookie."
  type        = string
  sensitive   = true
}
