variable "project_name" {
  description = "Project name used for resource names."
  type        = string
  default     = "tranquilai"
}

variable "region" {
  description = "DigitalOcean region for the ephemeral staging DOKS cluster."
  type        = string
  default     = "nyc3"
}

variable "kubernetes_version" {
  description = "DOKS Kubernetes version slug. Leave null to use the DigitalOcean default."
  type        = string
  default     = null
}

variable "node_size" {
  description = "DigitalOcean droplet size for the staging node pool."
  type        = string
  default     = "s-4vcpu-8gb"
}

variable "min_nodes" {
  description = "Minimum nodes in the staging node pool."
  type        = number
  default     = 1
}

variable "max_nodes" {
  description = "Maximum nodes in the staging node pool."
  type        = number
  default     = 1
}

variable "cluster_name_suffix" {
  description = "Unique suffix for the ephemeral cluster name, usually the Git SHA."
  type        = string
  default     = "manual"
}
