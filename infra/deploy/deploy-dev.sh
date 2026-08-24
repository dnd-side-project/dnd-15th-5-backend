#!/bin/bash
set -euo pipefail

APP_DIR="/app/chapchap-dev"
COMPOSE_FILE="$APP_DIR/docker-compose.dev.yml"
CADDY_NETWORK="chapchap_dev-proxy-net"

cd "$APP_DIR"

if ! docker network inspect "$CADDY_NETWORK" > /dev/null 2>&1; then
  echo "[deploy-dev] Caddy-Dev 공유 네트워크 $CADDY_NETWORK가 없습니다. Prod compose를 먼저 배포해 주세요."
  exit 1
fi

# ECR 로그인 (EC2 IAM Role의 ecr:GetAuthorizationToken 권한 사용)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "${ECR_IMAGE%%/*}"

export ECR_IMAGE
export DEV_TAG="$TAG"

echo "[deploy-dev] 배포 대상: $ECR_IMAGE:$DEV_TAG"

docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" up -d redis-dev
docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" pull app-dev
docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" up -d --no-deps app-dev

for i in $(seq 1 30); do
  STATUS="$(docker inspect --format='{{.State.Health.Status}}' app-dev 2>/dev/null || echo starting)"
  if [ "$STATUS" = "healthy" ]; then
    echo "[deploy-dev] app-dev healthy"
    if ! docker exec caddy wget -qO- http://app-dev:8080/actuator/health > /dev/null; then
      echo "[deploy-dev] caddy에서 app-dev로 연결할 수 없습니다. 공유 Docker network를 확인해 주세요."
      exit 1
    fi
    echo "[deploy-dev] caddy -> app-dev 연결 확인"
    echo "[deploy-dev] 배포 완료"
    exit 0
  fi
  if [ "$i" -eq 30 ]; then
    echo "[deploy-dev] app-dev 헬스체크 실패 (상태: $STATUS)"
    docker logs --tail 100 app-dev || true
    docker stop app-dev || true
    exit 1
  fi
  sleep 3
done
