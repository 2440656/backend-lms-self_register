locals {
  common_tags = {
    Project   = "mls"
    terraform = "true"
  }

  defender_tw_policy = "cnVsZV9uYW1lPURlZmF1bHQgLSBhbGVydCBvbiBzdXNwaWNpb3VzIHJ1bnRpbWUgYmVoYXZpb3IKYWR2YW5jZWRfcHJvdGVjdGlvbj10cnVlCnByb2Nlc3NfZWZmZWN0PWFsZXJ0CnByb2Nlc3NfYmxvY2tfYWxsPXRydWUKcHJvY2Vzc19kZXRlY3RfY3J5cHRvbWluZXJzPXRydWUKY29uc29sZV9ob3N0PXVzLWVhc3QxLmNsb3VkLnR3aXN0bG9jay5jb20KY29uc29sZV9wb3J0PTQ0Mwpjb25zb2xlX3RpbWVvdXRfc2VjPTMKY3VzdG9tZXJfaWQ9dXMtMS0xMTE1NzM0MjEKYXBpX2tleT1zQVdKZkc4bTFwYmtQcnFLVHMwNy82YWRMMjZNcEZrZS9lZnZUOVRuUFhXYzdnUG9YVTlIeUozMm9SVWUwRnN1S0h6NjR6WDlaYkg5L2ZmTXpTQlMwdz09CnBvbGljeV91cGRhdGVfaW50ZXJ2YWxfbXM9MTIwMDAwCg=="
}

module "lms_interceptor_service_dynamodb" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//microservices-terraform/modules/lms-dynamodb?ref=v0.5.7.9"
  providers = {
    aws.sharedservices = aws
  }

  dynamo_db_tables = var.dynamo_db_tables
}

module "vpc" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//microservices-terraform/modules/lms-vpc?ref=v0.5.7.9"
  providers = {
    aws.sharedservices = aws
  }
  count              = var.lambda_enable_vpc && !var.use_existing_vpc ? 1 : 0
  enable_nat_gateway = true
  lambda_vpc_name    = var.lambda_vpc_name
}

module "vpc-failover" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//microservices-terraform/modules/lms-vpc?ref=v0.5.7.9"
  providers = {
    aws                = aws.failover
    aws.sharedservices = aws.failover
  }
  count = var.lambda_enable_vpc && !var.use_existing_vpc_failover && var.configure_failover ? 1 : 0


  enable_nat_gateway = true
  lambda_vpc_name    = "${var.lambda_vpc_name}-dr"
}

module "lambda_layer_defender" {
  source = "terraform-aws-modules/lambda/aws"

  version                = "7.10.0"
  create_layer           = true
  layer_name             = "${var.defender_layer_name_prefix}-twistlock_nodejs"
  description            = "Twist lock Node JS defender layer"
  local_existing_package = var.defender_zipfile
  create_package         = false
}

module "lambda_layer_defender_failover" {
  source = "terraform-aws-modules/lambda/aws"
  count  = var.configure_failover ? 1 : 0

  version = "7.10.0"
  providers = {
    aws = aws.failover
  }

  create_layer           = true
  layer_name             = "${var.defender_layer_name_prefix}-twistlock_nodejs"
  description            = "Twist lock Node JS defender layer"
  local_existing_package = var.defender_zipfile
  create_package         = false
}

module "lambda_function" {
  depends_on = [
    module.lambda_layer_defender
  ]
  source  = "terraform-aws-modules/lambda/aws"
  version = "7.10.0"

  function_name = var.lambda-function-name
  description   = "Lambda function for processing DynamoDB stream events and saving audit logs."
  runtime       = "nodejs20.x"
  handler       = "index.handler"

  source_path         = "/tmp/lms-audit-logs-service/src"
  create_package      = true
  publish             = true
  create_sam_metadata = true

  vpc_subnet_ids = var.lambda_enable_vpc ? (
    var.use_existing_vpc ? var.existing_private_subnets : module.vpc[0].private_subnets
  ) : null
  vpc_security_group_ids = var.lambda_enable_vpc ? (
    var.use_existing_vpc ? var.existing_security_group_ids : [module.vpc[0].default_security_group_id]
  ) : null
  attach_network_policy = true
  policy_json = templatefile("${path.module}/lambda-policy-json.tftpl", {
    lambda_function_name = var.lambda-function-name
  })
  attach_policy_json    = true
  tracing_mode          = var.enable_lambda_tracing ? var.xray_tracing_lambda : null
  attach_tracing_policy = true

  tags = merge(
    { Name = var.lambda-function-name },
    local.common_tags
  )

  layers = [module.lambda_layer_defender.lambda_layer_arn]

  environment_variables = var.lambda_function_environment_variables
}

module "lambda_function_failover" {
  depends_on = [
    module.lambda_function
  ]
  source  = "terraform-aws-modules/lambda/aws"
  count   = var.configure_failover ? 1 : 0
  version = "7.10.0"

  providers = {
    aws = aws.failover
  }

  function_name = "${var.lambda-function-name}-failover"
  description   = "Failover Lambda for processing DynamoDB stream events and saving audit logs."
  runtime       = "nodejs18.x"
  handler       = "index.handler"

  source_path         = "/tmp/lms-audit-logs-service/src"
  create_package      = true
  publish             = true
  create_sam_metadata = true

  vpc_subnet_ids = var.lambda_enable_vpc ? (
    var.use_existing_vpc_failover ? var.existing_private_subnets_failover : module.vpc-failover[0].private_subnets
  ) : null
  vpc_security_group_ids = var.lambda_enable_vpc ? (
    var.use_existing_vpc_failover ? var.existing_security_group_ids_failover : [module.vpc-failover[0].default_security_group_id]
  ) : null
  attach_network_policy = true
  policy_json = templatefile("${path.module}/lambda-policy-json.tftpl", {
    lambda_function_name = "${var.lambda-function-name}-failover"
  })
  attach_policy_json    = true
  tracing_mode          = var.enable_lambda_tracing ? var.xray_tracing_lambda : null
  attach_tracing_policy = true

  tags = merge(
    { Name = "${var.lambda-function-name}-failover" },
    local.common_tags
  )

  layers = [module.lambda_layer_defender_failover[0].lambda_layer_arn]

  environment_variables = var.lambda_function_environment_variables
}