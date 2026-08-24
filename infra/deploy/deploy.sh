#!/bin/bash
set -euo pipefail

APP_DIR="/app/chapchap"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
UPSTREAM_FILE="$APP_DIR/upstream.caddy"
DEV_CONTAINER="app-dev"
DEV_HEALTH_TIMEOUT_SECONDS=90
DEV_HEALTH_POLL_INTERVAL_SECONDS=3
DEV_WAS_RUNNING=false
ACTIVE=""
STANDBY=""
STANDBY_STARTED=false
TRAFFIC_SWITCHED=false

restore_dev() {
  local exit_code=$?
  local dev_health_status=""
  local dev_health_deadline=0
  trap - EXIT INT TERM

  if [ "$exit_code" -ne 0 ] && [ "$STANDBY_STARTED" = true ] && [ "$TRAFFIC_SWITCHED" = false ]; then
    echo "[deploy] 실패한 app-$STANDBY 정리 및 app-$ACTIVE upstream 복구"
    echo "reverse_proxy app-$ACTIVE:8080" > "$UPSTREAM_FILE" || true
    docker exec caddy caddy reload --config /etc/caddy/Caddyfile || true
    docker stop "app-$STANDBY" || true
  fi

  if [ "$DEV_WAS_RUNNING" = true ]; then
    if docker start "$DEV_CONTAINER" > /dev/null; then
      dev_health_deadline=$((SECONDS + DEV_HEALTH_TIMEOUT_SECONDS))

      while (( SECONDS < dev_health_deadline )); do
        dev_health_status="$(docker inspect --format='{{if .State.Running}}{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}{{else}}{{.State.Status}}{{end}}' "$DEV_CONTAINER" 2>/dev/null || echo "inspect-failed")"

        case "$dev_health_status" in
          healthy)
            echo "[deploy] $DEV_CONTAINER 복구 완료"
            break
            ;;
          starting|restarting)
            sleep "$DEV_HEALTH_POLL_INTERVAL_SECONDS"
            ;;
          *)
            break
            ;;
        esac
      done

      if [ "$dev_health_status" != "healthy" ]; then
        echo "[deploy] $DEV_CONTAINER 복구 실패 (상태: ${dev_health_status:-timeout})"
        [ "$exit_code" -ne 0 ] || exit_code=1
      fi
    else
      echo "[deploy] $DEV_CONTAINER 복구 실패"
      [ "$exit_code" -ne 0 ] || exit_code=1
    fi
  fi

  exit "$exit_code"
}

trap restore_dev EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

cd "$APP_DIR"

# ECR 로그인 (EC2 IAM Role의 ecr:GetAuthorizationToken 권한 사용)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "${ECR_IMAGE%%/*}"

if [ -f "$UPSTREAM_FILE" ] && grep -q "app-green" "$UPSTREAM_FILE"; then
  ACTIVE=green
  STANDBY=blue
else
  ACTIVE=blue
  STANDBY=green
  # 최초 배포 부트스트랩: upstream.caddy가 없으면 기본값으로 생성
  [ -f "$UPSTREAM_FILE" ] || echo "reverse_proxy app-blue:8080" > "$UPSTREAM_FILE"
fi

echo "[deploy] 현재 활성: app-$ACTIVE / 배포 대상(standby): app-$STANDBY -> $ECR_IMAGE:$TAG"

TAG_VAR="$(echo "$STANDBY" | tr '[:lower:]' '[:upper:]')_TAG"
export ECR_IMAGE
declare -x "$TAG_VAR=$TAG"

# redis/caddy는 최초 배포 부트스트랩용. 이미 떠있으면 up -d가 아무 일도 안 함.
# --no-deps 필수: caddy의 depends_on(app-blue)까지 자동으로 끌려와서
# 아직 태그가 안 정해진 쪽(BLUE_TAG 기본값 latest)을 pull하려다 실패하는 걸 방지.
docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" up -d --no-deps redis caddy

docker compose -f "$COMPOSE_FILE" --env-file "$APP_DIR/.env" pull "app-$STANDBY"

if [ "$(docker inspect --format='{{.State.Running}}' "$DEV_CONTAINER" 2>/dev/null || true)" = "true" ]; then
  DEV_WAS_RUNNING=true
  echo "[deploy] Prod blue/green 기동을 위해 $DEV_CONTAINER 일시 중지"
  docker stop "$DEV_CONTAINER"
fi

STANDBY_STARTED=true
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
TRAFFIC_SWITCHED=true

echo "[deploy] 트래픽 전환 완료: app-$STANDBY 활성화"

docker stop "app-$ACTIVE" || true

echo "[deploy] 배포 완료"
