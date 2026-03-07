data "aws_caller_identity" "current" {}

module "s3_bucket" {
  source      = "github.com/Cognizant-SPE/lms-terraform-modules//s3_bucket?ref=v0.5.7.9"

  bucket_name = var.s3_bucket_name
  region      = var.aws_region
  additional_tags = {
    Environment = var.environment
  }
}

module "lms_backend" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//microservices-terraform?ref=v0.5.7.9"

  providers = {
    aws.sharedservices = aws.sharedservices
    aws.failover       = aws.failover
    aws.sharedservices-failover = aws.sharedservices-failover
  }
  region                     = var.aws_region 
  specfile                   = var.api_specfile_path
  apispec_substitutions_file = var.apispec_substitutions_file
  api_name_prefix            = var.api_name_prefix
  api_stage_name             = var.api_stage_name

  disable_execute_api_endpoint   = local.disable_execute_api_endpoint
  lambda_authorizer_functionname = local.lambda_authorizer_functionname

  domain_name                     = var.api_domain_name
  acm_certificate_arn             = local.acm_certificate_arn
  acm_certificate_failover_arn    = local.acm_certificate_failover_arn
  create_domain_entry             = local.create_domain_entry
  api_base_path                   = var.api_base_path
  create_route53_entry            = local.create_route53_entry
  hosted_zone_name                = local.hosted_zone_name

  attach_waf        = local.attach_waf
  web_acl           = local.web_acl
  api_endpoint_type = local.api_endpoint_type

  api_service_lambda_functionname = var.api_lambda_function_name

  lambda_function_ecr_repo_arn = var.repositoryarn
  lambda_function_image_uri    = "${var.repositoryuri}:${var.imageversion}"

  lambda_function_failover_ecr_repo_arn = "${var.repositoryarn_failover}"
  lambda_function_failover_image_uri = "${var.repositoryuri_failover}:${var.imageversion}"

  provisioned_concurrent_executions     = var.provisioned_concurrent_executions
  lambda_alias_name                     = var.lambda_alias_name
  lambda_function_environment_variables = local.lambda_environment_variables

  lambda_function_memory  = var.lambda_function_memory
  lambda_function_timeout = var.lambda_function_timeout
  lambda_enable_vpc       = local.lambda_enable_vpc
  enable_nat_gateway      = local.enable_nat_gateway

  lambda_vpc_name         = var.lambda_vpc_name
  configure_failover      = local.configure_failover
  
  xray_tracing_lambda        = var.xray_tracing_lambda
  xray_tracing_enabled_api   = var.xray_tracing_enabled_api
  ### DynamoDB Tables
  dynamo_db_tables = var.dynamo_db_tables
 ##Adding Datadog Forwarder variables 

  enable_datadog_forwarder          = var.enable_datadog_forwarder
  enable_datadog_forwarder_failover = var.enable_datadog_forwarder_failover
  datadog_forwarder_arn             = var.datadog_forwarder_arn
  datadog_forwarder_arn_failover    = var.datadog_forwarder_arn_failover
  log_retention_in_days             = var.log_retention_in_days

  ##### Existing VPC Variables #####
  use_existing_vpc = var.use_existing_vpc
  existing_vpc_id = var.existing_vpc_id
  existing_private_subnets = var.existing_private_subnets
  existing_security_group_ids = var.existing_security_group_ids

  use_existing_vpc_failover = var.use_existing_vpc_failover
  existing_vpc_id_failover = var.existing_vpc_id_failover
  existing_private_subnets_failover = var.existing_private_subnets_failover
  existing_security_group_ids_failover = var.existing_security_group_ids_failover
}
data "aws_secretsmanager_secret_version" "fetched_secret" {
  secret_id = var.lms_credentials
}