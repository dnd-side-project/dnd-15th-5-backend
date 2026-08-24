#!/bin/bash
set -euo pipefail

COMMAND_ID="${1:?COMMAND_ID가 필요합니다.}"
INSTANCE_ID="${2:?INSTANCE_ID가 필요합니다.}"

POLL_MAX_ATTEMPTS=60
POLL_INTERVAL_SECONDS=10
CANCEL_MAX_ATTEMPTS=12
CANCEL_POLL_INTERVAL_SECONDS=5

get_status() {
  aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --query Status \
    --output text 2>/dev/null || echo "Pending"
}

is_terminal() {
  case "$1" in
    Success|Failed|Cancelled|TimedOut) return 0 ;;
    *) return 1 ;;
  esac
}

status="InProgress"
poll_timed_out=false

for poll_attempt in $(seq 1 "$POLL_MAX_ATTEMPTS"); do
  status="$(get_status)"
  is_terminal "$status" && break
  [ "$poll_attempt" -eq "$POLL_MAX_ATTEMPTS" ] || sleep "$POLL_INTERVAL_SECONDS"
done

if ! is_terminal "$status"; then
  poll_timed_out=true
  echo "SSM 명령 polling 시간 초과, 취소를 요청합니다: $status" >&2
  if ! aws ssm cancel-command --command-id "$COMMAND_ID" --instance-ids "$INSTANCE_ID" > /dev/null; then
    echo "SSM 명령 취소 요청 실패" >&2
  fi

  for cancel_attempt in $(seq 1 "$CANCEL_MAX_ATTEMPTS"); do
    status="$(get_status)"
    is_terminal "$status" && break
    [ "$cancel_attempt" -eq "$CANCEL_MAX_ATTEMPTS" ] || sleep "$CANCEL_POLL_INTERVAL_SECONDS"
  done
fi

printf '%s\n' "$status"

if [ "$poll_timed_out" = true ] || [ "$status" != "Success" ]; then
  exit 1
fi
