#!/bin/bash
set -euo pipefail

APP_DIR="/app/chapchap"
LIVE_DIR="$APP_DIR/frontend"
NEW_DIR="$APP_DIR/frontend_new"

rm -rf "$NEW_DIR"
mkdir -p "$NEW_DIR"

aws s3 sync "s3://${FRONTEND_BUCKET}/dist/" "$NEW_DIR" --delete

if [ ! -f "$NEW_DIR/index.html" ]; then
  echo "[deploy-frontend] index.html이 없음 — S3 업로드 결과가 비정상적, 배포 중단"
  rm -rf "$NEW_DIR"
  exit 1
fi

rm -rf "$LIVE_DIR"
mv "$NEW_DIR" "$LIVE_DIR"

echo "[deploy-frontend] 프론트 정적 파일 교체 완료 ($LIVE_DIR)"
