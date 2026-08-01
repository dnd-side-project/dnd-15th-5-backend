terraform {
  backend "s3" {
    bucket       = "dnd-terraform-state-183537898450"
    key          = "aws/terraform.tfstate"
    region       = "ap-northeast-2"
    encrypt      = true
    use_lockfile = true
  }
}
