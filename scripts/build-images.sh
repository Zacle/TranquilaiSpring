#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-ghcr.io}"
NAMESPACE="${NAMESPACE:-zacle}"
IMAGE_PREFIX="${IMAGE_PREFIX:-tranquilai-}"
TAG="${TAG:-local}"
PUSH=false
NO_CACHE=false

SERVICES=(
  "api-gateway"
  "auth-service"
  "user-service"
  "ai-service"
  "content-service"
  "activity-service"
  "plan-service"
  "progress-service"
  "notification-service"
  "subscription-service"
)

usage() {
  cat <<EOF
Usage: scripts/build-images.sh [options]

Options:
  -t, --tag TAG          Image tag to build. Default: local
  -n, --namespace NAME   GHCR namespace/user. Default: zacle
  -r, --registry HOST    Registry host. Default: ghcr.io
  --prefix PREFIX        Image name prefix. Default: tranquilai-
  --push                 Push images after building
  --no-cache             Build without Docker cache
  -h, --help             Show this help

Examples:
  scripts/build-images.sh
  scripts/build-images.sh --tag "\$(git rev-parse --short HEAD)"
  scripts/build-images.sh --tag "\$(git rev-parse HEAD)" --push

Before pushing to GHCR:
  echo "\$GITHUB_TOKEN" | docker login ghcr.io -u zacle --password-stdin
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--tag)
      TAG="$2"
      shift 2
      ;;
    -n|--namespace)
      NAMESPACE="$2"
      shift 2
      ;;
    -r|--registry)
      REGISTRY="$2"
      shift 2
      ;;
    --prefix)
      IMAGE_PREFIX="$2"
      shift 2
      ;;
    --push)
      PUSH=true
      shift
      ;;
    --no-cache)
      NO_CACHE=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but was not found on PATH." >&2
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"

for SERVICE in "${SERVICES[@]}"; do
  DOCKERFILE="$ROOT_DIR/$SERVICE/Dockerfile"
  if [[ ! -f "$DOCKERFILE" ]]; then
    echo "Dockerfile not found for $SERVICE at $DOCKERFILE" >&2
    exit 1
  fi

  IMAGE="${REGISTRY}/${NAMESPACE}/${IMAGE_PREFIX}${SERVICE}:${TAG}"
  IMAGE="$(echo "$IMAGE" | tr '[:upper:]' '[:lower:]')"

  echo "Building $IMAGE"
  BUILD_ARGS=(build -t "$IMAGE" -f "$DOCKERFILE")
  if [[ "$NO_CACHE" == true ]]; then
    BUILD_ARGS+=(--no-cache)
  fi
  BUILD_ARGS+=("$ROOT_DIR")

  docker "${BUILD_ARGS[@]}"

  if [[ "$PUSH" == true ]]; then
    echo "Pushing $IMAGE"
    docker push "$IMAGE"
  fi
done

echo "Built ${#SERVICES[@]} images with tag '$TAG'."

if [[ "$PUSH" != true ]]; then
  echo "Run again with --push after logging in to GHCR to publish them."
fi
