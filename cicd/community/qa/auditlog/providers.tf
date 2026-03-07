terraform {
  required_version = ">= 1.3"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.0.0"
    }
    local = ">= 1.4"
  }
}

provider "aws" {
  alias  = "failover"
  region = var.failover_region
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_account_id}:role/${var.aws_account_role}"
  }
}

provider "aws" {
  region = var.region
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_account_id}:role/${var.aws_account_role}"
  }
}