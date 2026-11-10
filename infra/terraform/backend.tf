terraform {
  backend "s3" {
    bucket       = "chapchap-terraform-state-631121070981"
    key          = "aws/terraform.tfstate"
    region       = "ap-northeast-2"
    encrypt      = true
    use_lockfile = true
    allowed_account_ids = ["631121070981"]
  }
}
