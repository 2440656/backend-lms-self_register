locals {
  current_account_id = data.aws_caller_identity.current.account_id

  common_tags = {
    Project   = "mls"
    terraform = "true"
  }

  disable_execute_api_endpoint   = true
  lambda_authorizer_functionname = "prod-mls-lambda-authorizer"
  api_endpoint_type              = "EDGE"

  attach_waf = true
  web_acl    = "mls-waf-regional"
  web_acl_cloudfront  = "mls-waf-global" 
  
  acm_certificate_arn_cloudfront ="arn:aws:acm:us-east-1:478340992552:certificate/83f1cf68-6768-4542-afed-ee5013b48b6b"
  acm_certificate_arn  = "arn:aws:acm:ap-south-1:478340992552:certificate/4b44cf2c-2025-4fbf-acff-c3bce7cdc79c"
  acm_certificate_failover_arn  = "arn:aws:acm:us-east-1:478340992552:certificate/83f1cf68-6768-4542-afed-ee5013b48b6b"
  create_domain_entry  = true
  create_route53_entry = true

  hosted_zone_name   = "myskillspring.cognizant.com"
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