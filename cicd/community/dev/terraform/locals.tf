locals {
    current_account_id = data.aws_caller_identity.current.account_id

    common_tags = {
        Project   = "lms"
        terraform = "true"
    }

    disable_execute_api_endpoint   = true
    lambda_authorizer_functionname = "dev-lms-lambda-authorizer"
    api_endpoint_type = "EDGE"

    attach_waf          = true
    web_acl             = "lms-waf-regional" 
    web_acl_cloudfront  = "lms-waf-global"  

  acm_certificate_arn  = "arn:aws:acm:us-east-1:050752635551:certificate/0321883d-5a2b-478b-864e-373d94ce6343"
  acm_certificate_failover_arn  = "arn:aws:acm:us-west-2:050752635551:certificate/d8b24422-a4bb-4c1d-88da-faa42d34d12e"
  create_domain_entry  = true
  create_route53_entry = true

  hosted_zone_name     = "dev.lms.cognizant.com"
  lambda_enable_vpc    = true
  enable_nat_gateway   = true 
  configure_failover   = true
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