# TranquilAI Terraform

Terraform owns the cloud and platform layer:

- DigitalOcean Project for grouping TranquilAI resources
- DigitalOcean Kubernetes cluster
- staging and production namespaces
- ingress-nginx
- cert-manager
- kube-prometheus-stack
- Cloudflare DNS records for staging and production

Application deployments are intentionally left to Helm/GitHub Actions so image rollouts do not require a Terraform apply.

## State

Use a remote backend before applying this outside a disposable test account. DigitalOcean Spaces can be used through Terraform's S3 backend.

Example backend file:

```hcl
bucket                      = "tranquilai-terraform-state"
key                         = "backend/platform.tfstate"
region                      = "us-east-1"
endpoint                    = "https://nyc3.digitaloceanspaces.com"
skip_credentials_validation = true
skip_metadata_api_check     = true
skip_region_validation      = true
skip_requesting_account_id  = true
```

Initialize with:

```powershell
terraform init -backend-config=backend.hcl
```

## Credentials

Do not commit credentials. Use environment variables:

```powershell
$env:DIGITALOCEAN_TOKEN = "<do-token>"
$env:CLOUDFLARE_API_TOKEN = "<cloudflare-token>"
```

## Apply

```powershell
cd infra/terraform
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

The staging and production ingresses both use `letsencrypt-prod`. Verify DNS points to the ingress load balancer before deploying the app chart.

## DigitalOcean Project

Terraform creates a DigitalOcean Project and assigns the DOKS cluster to it:

```hcl
digitalocean_project_name = "TranquilAI"
```

If you already created a Project manually, either use a different name or import it before applying:

```powershell
terraform import digitalocean_project.tranquilai <project-id>
```

Kubernetes-created resources such as the ingress load balancer are created by DigitalOcean after the Kubernetes Service is applied. If a load balancer still appears in the default Project, move it once from the DigitalOcean dashboard after it exists.

## Secret Boundary

Terraform creates platform secrets required by platform Helm charts:

- Cloudflare token for cert-manager DNS-01
- Grafana admin credentials
These values are stored in Terraform state. Use a private encrypted remote state backend and treat state access as secret access.

Terraform does not create application secrets. Keep app secrets in SOPS files under `infra/k8s/secrets`, or later move them to a dedicated secret-management layer such as External Secrets Operator. RabbitMQ is managed externally through CloudAMQP and configured through the application SOPS secret.
