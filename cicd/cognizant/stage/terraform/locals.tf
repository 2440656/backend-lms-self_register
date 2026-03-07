locals {
  current_account_id = data.aws_caller_identity.current.account_id

  common_tags = {
    Project   = "mls"
    terraform = "true"
  }

  disable_execute_api_endpoint   = true
  lambda_authorizer_functionname = "stage-mls-lambda-authorizer"
  api_endpoint_type              = "EDGE"


  attach_waf = true
  web_acl    = "mls-waf-regional"
  web_acl_cloudfront  = "mls-waf-global"
  acm_certificate_arn_cloudfront = "arn:aws:acm:us-east-1:336544441798:certificate/d7d2d8b7-8fdd-4131-a922-70d6816f7dc8"
  acm_certificate_arn  = "arn:aws:acm:ap-south-1:336544441798:certificate/99d9957c-a533-4c63-9ddd-2b57b5516526"
  acm_certificate_failover_arn  = "arn:aws:acm:us-east-1:336544441798:certificate/d7d2d8b7-8fdd-4131-a922-70d6816f7dc8"
  create_domain_entry  = true
  create_route53_entry = true

  hosted_zone_name   = "stage-mls.lms.cognizant.com"
  lambda_enable_vpc  = true
  enable_nat_gateway = true
  configure_failover = true
  cloudfront_configure_failover = false

  secret_values = jsondecode(data.aws_secretsmanager_secret_version.fetched_secret.secret_string)

  #Dynamically merge DD_VERSION from imageversion
  lambda_environment_variables = merge(
  {
    DD_VERSION = var.imageversion
    AWS_COGNITO_CLIENT_ID   = local.secret_values.AWS_COGNITO_CLIENT_ID
    AWS_COGNITO_USER_POOL_ID = local.secret_values.AWS_COGNITO_USER_POOL_ID
    aws_cognito_user_pool_id = local.secret_values.AWS_COGNITO_USER_POOL_ID
   },
    var.lambda_function_environment_variables
  )
}
