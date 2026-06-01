resource "kubernetes_secret_v1" "grafana_cloud_prometheus" {
  metadata {
    name      = "grafana-cloud-prometheus"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }

  data = {
    GRAFANA_CLOUD_PROMETHEUS_REMOTE_WRITE_URL = var.grafana_cloud_prometheus_remote_write_url
    GRAFANA_CLOUD_PROMETHEUS_USERNAME         = var.grafana_cloud_prometheus_username
    GRAFANA_CLOUD_PROMETHEUS_PASSWORD         = var.grafana_cloud_prometheus_password
  }
}

resource "kubernetes_config_map_v1" "grafana_alloy_config" {
  metadata {
    name      = "grafana-alloy-config"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }

  data = {
    "config.alloy" = <<-EOT
      discovery.kubernetes "tranquilai_prod_pods" {
        role = "pod"

        namespaces {
          own_namespace = false
          names         = ["${local.prod_namespace}"]
        }
      }

      discovery.relabel "tranquilai_prod_metrics" {
        targets = discovery.kubernetes.tranquilai_prod_pods.targets

        rule {
          source_labels = ["__meta_kubernetes_pod_phase"]
          regex         = "Running"
          action        = "keep"
        }

        rule {
          source_labels = ["__meta_kubernetes_pod_container_port_name"]
          regex         = "http"
          action        = "keep"
        }

        rule {
          target_label = "__metrics_path__"
          replacement  = "/actuator/prometheus"
        }

        rule {
          source_labels = ["__meta_kubernetes_pod_ip", "__meta_kubernetes_pod_container_port_number"]
          separator     = ":"
          target_label  = "__address__"
        }

        rule {
          source_labels = ["__meta_kubernetes_pod_label_app_kubernetes_io_name"]
          target_label  = "service"
        }

        rule {
          source_labels = ["__meta_kubernetes_namespace"]
          target_label  = "namespace"
        }

        rule {
          source_labels = ["__meta_kubernetes_pod_name"]
          target_label  = "pod"
        }
      }

      prometheus.scrape "tranquilai_prod" {
        targets         = discovery.relabel.tranquilai_prod_metrics.output
        forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
        scrape_interval = "30s"
      }

      prometheus.remote_write "grafana_cloud" {
        endpoint {
          url = sys.env("GRAFANA_CLOUD_PROMETHEUS_REMOTE_WRITE_URL")

          basic_auth {
            username = sys.env("GRAFANA_CLOUD_PROMETHEUS_USERNAME")
            password = sys.env("GRAFANA_CLOUD_PROMETHEUS_PASSWORD")
          }
        }
      }
    EOT
  }
}

resource "kubernetes_service_account_v1" "grafana_alloy" {
  metadata {
    name      = "grafana-alloy"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }
}

resource "kubernetes_cluster_role_v1" "grafana_alloy" {
  metadata {
    name = "grafana-alloy"
  }

  rule {
    api_groups = [""]
    resources  = ["pods", "nodes", "nodes/proxy", "services", "endpoints"]
    verbs      = ["get", "list", "watch"]
  }
}

resource "kubernetes_cluster_role_binding_v1" "grafana_alloy" {
  metadata {
    name = "grafana-alloy"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = kubernetes_cluster_role_v1.grafana_alloy.metadata[0].name
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account_v1.grafana_alloy.metadata[0].name
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }
}

resource "kubernetes_deployment_v1" "grafana_alloy" {
  metadata {
    name      = "grafana-alloy"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
    labels = {
      app = "grafana-alloy"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "grafana-alloy"
      }
    }

    template {
      metadata {
        labels = {
          app = "grafana-alloy"
        }
      }

      spec {
        service_account_name = kubernetes_service_account_v1.grafana_alloy.metadata[0].name

        container {
          name  = "alloy"
          image = "grafana/alloy:v1.16.1"

          args = [
            "run",
            "--storage.path=/var/lib/alloy/data",
            "--server.http.listen-addr=0.0.0.0:12345",
            "/etc/alloy/config.alloy",
          ]

          env_from {
            secret_ref {
              name = kubernetes_secret_v1.grafana_cloud_prometheus.metadata[0].name
            }
          }

          port {
            name           = "http"
            container_port = 12345
          }

          volume_mount {
            name       = "config"
            mount_path = "/etc/alloy"
            read_only  = true
          }

          volume_mount {
            name       = "storage"
            mount_path = "/var/lib/alloy/data"
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "128Mi"
            }

            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }
        }

        volume {
          name = "config"

          config_map {
            name = kubernetes_config_map_v1.grafana_alloy_config.metadata[0].name
          }
        }

        volume {
          name = "storage"

          empty_dir {}
        }
      }
    }
  }
}
