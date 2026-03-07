locals {
    current_account_id = data.aws_caller_identity.current.account_id

    common_tags = {
        Project   = "lms"
        terraform = "true"
    }

    disable_execute_api_endpoint   = true
    lambda_authorizer_functionname = "sbx-lms-lambda-authorizer"
    api_endpoint_type = "EDGE"

    attach_waf          = true
    web_acl             = "lms-waf-regional" 
    web_acl_cloudfront  = "lms-waf-global"  

  acm_certificate_arn  = "arn:aws:acm:us-east-1:885657071929:certificate/c34e1550-71af-4844-958d-9aeaf2a5d3a4"
  create_domain_entry  = true
  create_route53_entry = true

  hosted_zone_name     = "sbx.lms.cognizant.com"
  lambda_enable_vpc    = true
  enable_nat_gateway   = true 
  configure_failover   = false
  cloudfront_configure_failover = false

  secret_values = jsondecode(data.aws_secretsmanager_secret_version.fetched_secret.secret_string)

  #Dynamically merge DD_VERSION from imageversion
  lambda_environment_variables = merge(
  {
    AWS_COGNITO_CLIENT_ID   = local.secret_values.AWS_COGNITO_CLIENT_ID
    AWS_COGNITO_USER_POOL_ID = local.secret_values.AWS_COGNITO_USER_POOL_ID
    aws_cognito_user_pool_id = local.secret_values.AWS_COGNITO_USER_POOL_ID
    
   },
    var.lambda_function_environment_variables
  )
}