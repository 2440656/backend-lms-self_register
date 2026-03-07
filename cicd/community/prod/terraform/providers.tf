provider "aws" {
  region = var.aws_region
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_account_id}:role/${var.aws_account_role}"
  }
}

provider "aws" {
  alias  = "replica"
  region = var.aws_replica_region
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_account_id}:role/${var.aws_account_role}"
  }
}

// Lambda edge needs to be created in us-east-1
provider "aws" {
  alias  = "lambdaedge"
  region = "us-east-1"
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_account_id}:role/${var.aws_account_role}"
  }
}

provider "aws" {
  alias = "sharedservices"
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_shared_services_account_id}:role/${var.aws_account_role}"
  }
}

provider "aws" {
  alias  = "failover"
  region = var.aws_failover_region
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_account_id}:role/${var.aws_account_role}"
  }
}

provider "aws" {
  alias  = "sharedservices-failover"
  region = var.aws_failover_region
  assume_role {
    role_arn = "arn:aws:iam::${var.aws_shared_services_account_id}:role/${var.aws_account_role}"
  }
}