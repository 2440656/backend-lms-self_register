data "aws_caller_identity" "current" {}

module "s3_bucket" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//s3_bucket?ref=v0.5.7.9"

  bucket_name = var.s3_bucket_name
  region      = var.aws_region
  additional_tags = {
    Environment = var.environment
  }
}

module "lms_backend" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//microservices-terraform?ref=ecs-backend-core"

  providers = {
    aws.sharedservices          = aws.sharedservices
    aws.failover                = aws.sharedservices
    aws.sharedservices-failover = aws.sharedservices
  }

  specfile                   = var.api_specfile_path
  apispec_substitutions_file = var.apispec_substitutions_file
  api_name_prefix            = var.api_name_prefix
  api_stage_name             = var.api_stage_name

  disable_execute_api_endpoint   = local.disable_execute_api_endpoint
  lambda_authorizer_functionname = local.lambda_authorizer_functionname

  domain_name          = var.api_domain_name
  acm_certificate_arn  = local.acm_certificate_arn
  create_domain_entry  = local.create_domain_entry
  api_base_path        = var.api_base_path
  create_route53_entry = local.create_route53_entry
  hosted_zone_name     = local.hosted_zone_name

  attach_waf        = local.attach_waf
  web_acl           = local.web_acl
  api_endpoint_type = local.api_endpoint_type

  api_service_lambda_functionname = var.api_lambda_function_name

  lambda_function_ecr_repo_arn = var.repositoryarn
  lambda_function_image_uri    = "${var.repositoryuri}:${var.imageversion}"

  lambda_function_failover_ecr_repo_arn = var.repositoryarn
  lambda_function_failover_image_uri    = "${var.repositoryuri}:${var.imageversion}"

  provisioned_concurrent_executions     = var.provisioned_concurrent_executions
  lambda_alias_name                     = var.lambda_alias_name
  lambda_function_environment_variables = local.lambda_environment_variables

  lambda_function_memory  = var.lambda_function_memory
  lambda_function_timeout = var.lambda_function_timeout
  lambda_enable_vpc       = local.lambda_enable_vpc
  enable_nat_gateway      = local.enable_nat_gateway

  lambda_vpc_name          = var.lambda_vpc_name
  configure_failover       = local.configure_failover
  xray_tracing_lambda      = var.xray_tracing_lambda
  xray_tracing_enabled_api = var.xray_tracing_enabled_api
  ### DynamoDB Tables
  dynamo_db_tables = var.dynamo_db_tables

  ##### Existing VPC Variables #####
  use_existing_vpc            = var.use_existing_vpc
  existing_vpc_id             = var.existing_vpc_id
  existing_private_subnets    = var.existing_private_subnets
  existing_security_group_ids = var.existing_security_group_ids

  ###### ECS backend service deployment ############

  aws_region       = var.aws_region
  ecs_service_name = var.ecs_service_name

  ecs_servicetask_ecr_repo_arn = var.repositoryarn
  ecs_servicetask_image_uri    = "${var.repositoryuri}:${var.ecs_imageversion}"

  ecs_servicetask_failover_ecr_repo_arn = var.repositoryarn
  ecs_servicetask_failover_image_uri    = "${var.repositoryuri}:${var.ecs_imageversion}"

  ecs_cluster_id              = var.ecs_cluster_id
  container_port              = var.container_port
  ecs_container_cpu           = var.ecs_container_cpu
  ecs_container_memory        = var.ecs_container_memory
  execution_role_arn          = var.execution_role_arn
  ecs_desired_container_count = var.ecs_desired_container_count
  enable_execute_command      = var.enable_execute_command
  #container_environment      = var.container_environment
  #container_secrets          = var.container_secrets
  alb_arn = var.alb_arn

  # New VPC variables - shared VPC
  existing_vpc_id_1             = var.existing_vpc_id_1
  existing_private_subnets_1    = var.existing_private_subnets_1
  existing_security_group_ids_1 = var.existing_security_group_ids_1

  # ECS Task IAM Permissions
  s3_bucket_arns = [
    "${module.s3_bucket.bucket_arn}",
    "${module.s3_bucket.bucket_arn}/*",
    "arn:aws:s3:::${var.s3_icons_bucket_name}",
    "arn:aws:s3:::${var.s3_icons_bucket_name}/*"
  ]
  eventbridge_bus_arn    = var.user_management_event_bus_arn
  cognito_userpool_arn   = var.cognito_userpool_arn
  cross_account_role_arn = var.cognito_cross_account_role_arn
  sqs_queue_arn          = var.sqs_queue_arn

  # Cross-account DynamoDB access (tenant config table in shared account)
  cross_account_dynamodb_arns = [
    "arn:aws:dynamodb:${var.aws_region}:${var.tenant_management_account}:table/${var.tenant_table}",
    "arn:aws:dynamodb:${var.aws_region}:${var.tenant_management_account}:table/${var.tenant_table}/index/*",
    "arn:aws:dynamodb:${var.aws_failover_region}:${var.tenant_management_account}:table/${var.tenant_table}",
    "arn:aws:dynamodb:${var.aws_failover_region}:${var.tenant_management_account}:table/${var.tenant_table}/index/*"
  ]

}

data "aws_secretsmanager_secret_version" "fetched_secret" {
  secret_id = var.lms_credentials
}
