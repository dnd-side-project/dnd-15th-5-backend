#!/bin/bash
set -euo pipefail

APP_DIR="/app/chapchap"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
UPSTREAM_FILE="$APP_DIR/upstream.caddy"

cd "$APP_DIR"

# ECR 로그인 (EC2 IAM Role의 ecr:GetAuthorizationToken 권한 사용)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "${ECR_IMAGE%%/*}"

if [ -f "$UPSTREAM_FILE" ] && grep -q "app-green" "$UPSTREAM_FILE"; then
  ACTIVE=green
  STANDBY=blue
else
  ACTIVE=blue
  STANDBY=green
fi

echo "[deploy] 현재 활성: app-$ACTIVE / 배포 대상(standby): app-$STANDBY -> $ECR_IMAGE:$TAG"

TAG_VAR="$(echo "$STANDBY" | tr '[:lower:]' '[:upper:]')_TAG"
export ECR_IMAGE
declare -x "$TAG_VAR=$TAG"

docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" pull "app-$STANDBY"
docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" up -d --no-deps "app-$STANDBY"

# 헬스체크
for i in $(seq 1 20); do
  STATUS="$(docker inspect --format='{{.State.Health.Status}}' "app-$STANDBY" 2>/dev/null || echo starting)"
  if [ "$STATUS" = "healthy" ]; then
    echo "[deploy] app-$STANDBY healthy"
    break
  fi
  if [ "$i" -eq 20 ]; then
    echo "[deploy] app-$STANDBY 헬스체크 실패 (상태: $STATUS) — 배포 중단, app-$ACTIVE 계속 서비스"
    docker logs --tail 100 "app-$STANDBY" || true
    exit 1
  fi
  sleep 3
done

echo "reverse_proxy app-$STANDBY:8080" > "$UPSTREAM_FILE"
docker exec caddy caddy reload --config /etc/caddy/Caddyfile

echo "[deploy] 트래픽 전환 완료: app-$STANDBY 활성화"

docker stop "app-$ACTIVE" || true

echo "[deploy] 배포 완료"
