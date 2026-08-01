output "ec2_public_ip" {
  description = "EC2 퍼블릭 IP "
  value       = aws_eip.app.public_ip
}

output "rds_endpoint" {
  description = "RDS 엔드포인트"
  value       = aws_db_instance.main.endpoint
}

output "s3_bucket_name" {
  description = "영수증 S3 버킷 이름"
  value       = aws_s3_bucket.receipts.bucket
}

output "ecr_repository_url" {
  description = "ECR 리포지토리 URL (docker-compose.prod.yml의 IMAGE 값으로 사용)"
  value       = aws_ecr_repository.backend.repository_url
}

output "github_actions_role_arn" {
  description = "GitHub Actions가 assume할 IAM Role ARN (워크플로우의 role-to-assume 값으로 사용)"
  value       = aws_iam_role.github_actions.arn
}

output "ec2_instance_id" {
  description = "EC2 인스턴스 ID (SSM send-command 대상)"
  value       = aws_instance.app.id
}
