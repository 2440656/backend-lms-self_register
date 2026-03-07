terraform {
  required_version = "~> 1.3"

  backend "s3" {
    # S3 Bucket
    bucket = "lms-pltools-terraform-states-s3"
    key    = "terraform-states/lms-platform/dev/lms-user-service.tfstate"
    region = "us-east-1"

    encrypt      = true
    use_lockfile = true

    # DynamoDB Table
    # dynamodb_table = "lms-pltools-terraform-locks"
    # encrypt        = true
  }
}
