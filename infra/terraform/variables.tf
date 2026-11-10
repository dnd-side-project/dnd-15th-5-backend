variable "aws_region" {
  description = "aws_region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "리소스 이름에 붙일 프로젝트 접두사"
  type        = string
  default     = "chapchap"
}

variable "vpc_cidr" {
  description = "VPC CIDR 대역"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "Public Subnet CIDR "
  type        = string
  default     = "10.0.1.0/24"
}

variable "private_subnet_cidr" {
  description = "Private Subnet CIDR "
  type        = string
  default     = "10.0.2.0/24"
}

variable "private_subnet_cidr_b" {
  description = "Private Subnet CIDR 2 (RDS는 최소 2개 가용영역 서브넷 그룹 필요)"
  type        = string
  default     = "10.0.3.0/24"
}

variable "db_snapshot_identifier" {
  description = "RDS 복원에 사용할 스냅샷 이름"
  type        = string
  default     = "chapchap-migrated-20261110"
  nullable    = false

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9-]{0,254}$", var.db_snapshot_identifier))
    error_message = "올바른 스냅샷 이름을 입력하세요. ARN은 사용할 수 없습니다."
  }
}
