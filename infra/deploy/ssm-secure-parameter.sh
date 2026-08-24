#!/bin/bash
set -euo pipefail

put_secure_parameter() {
  local parameter_name="${1:?Parameter 이름이 필요합니다.}"
  local kms_key_id="${2:?KMS Key ID가 필요합니다.}"
  local parameter_value

  parameter_value="$(cat)"

  aws ssm put-parameter \
    --name "$parameter_name" \
    --value "$parameter_value" \
    --type SecureString \
    --key-id "$kms_key_id" \
    --tier Standard \
    --overwrite > /dev/null
}

write_secure_parameter_to_file() {
  local parameter_name="${1:?Parameter 이름이 필요합니다.}"
  local output_file="${2:?출력 파일 경로가 필요합니다.}"
  local aws_region="${3:?AWS Region이 필요합니다.}"
  local temp_file

  umask 077
  mkdir -p "$(dirname "$output_file")"
  temp_file="$(mktemp "${output_file}.XXXXXX")"

  if ! aws ssm get-parameter \
    --region "$aws_region" \
    --name "$parameter_name" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text > "$temp_file"; then
    rm -f "$temp_file"
    return 1
  fi

  chmod 600 "$temp_file"
  mv "$temp_file" "$output_file"
}
