output "ec2_public_ip" {
  description = "EC2 고정 퍼블릭 IP (Elastic IP, 재시작해도 안 바뀜)"
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
