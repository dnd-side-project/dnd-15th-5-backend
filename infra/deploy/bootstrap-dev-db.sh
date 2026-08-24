#!/bin/bash
set -euo pipefail

required_variables=(
  DB_HOST
  DB_ADMIN_USERNAME
  DB_ADMIN_PASSWORD
  DEV_DB_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  if [ -z "${!variable_name:-}" ]; then
    echo "[bootstrap-dev-db] 필수 환경변수가 없습니다: $variable_name"
    exit 1
  fi
done

DB_PORT="${DB_PORT:-5432}"
DEV_DB_NAME="${DEV_DB_NAME:-chapchap_dev}"
DEV_DB_USERNAME="${DEV_DB_USERNAME:-chapchap_dev}"

for identifier in "$DEV_DB_NAME" "$DEV_DB_USERNAME"; do
  if [[ ! "$identifier" =~ ^[a-z_][a-z0-9_]*$ ]]; then
    echo "[bootstrap-dev-db] PostgreSQL 식별자 형식이 올바르지 않습니다: $identifier"
    exit 1
  fi
done

run_psql() {
  local database_name="$1"

  docker run --rm -i \
    -e PGPASSWORD="$DB_ADMIN_PASSWORD" \
    -e DEV_DB_NAME="$DEV_DB_NAME" \
    -e DEV_DB_USERNAME="$DEV_DB_USERNAME" \
    -e DEV_DB_PASSWORD="$DEV_DB_PASSWORD" \
    postgres:17-alpine \
    psql \
      --host "$DB_HOST" \
      --port "$DB_PORT" \
      --username "$DB_ADMIN_USERNAME" \
      --dbname "$database_name" \
      --no-password \
      --set ON_ERROR_STOP=1
}

run_psql postgres <<'SQL'
\getenv dev_db_name DEV_DB_NAME
\getenv dev_db_username DEV_DB_USERNAME
\getenv dev_db_password DEV_DB_PASSWORD

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'dev_db_username', :'dev_db_password')
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = :'dev_db_username'
)
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'dev_db_username', :'dev_db_password')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'dev_db_name', :'dev_db_username')
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = :'dev_db_name'
)
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'dev_db_name', :'dev_db_username')
\gexec
SQL

run_psql "$DEV_DB_NAME" <<'SQL'
CREATE EXTENSION IF NOT EXISTS postgis;
SQL

echo "[bootstrap-dev-db] $DEV_DB_NAME 데이터베이스와 $DEV_DB_USERNAME 계정 준비 완료"
