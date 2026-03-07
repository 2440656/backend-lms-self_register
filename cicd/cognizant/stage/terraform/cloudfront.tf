# Edge Lambda Auth Checks
module "auth_lambda_module" {
  source = "github.com/Cognizant-SPE/lms-terraform-modules//lambda-edge-authchecks?ref=v0.5.8.0"

  backendregion             = var.backendregion
  userpoolregion            = var.userpoolregion
  cookieExpirationDays      = var.cookieExpirationDays
  cookieDomain              = var.cookieDomain
  idTokenExpirationDays     = var.idTokenExpirationDays
  check-auth-function-name  = var.check_auth_function_name
  tenant_table              = var.tenant_table
  tenant_management_account = var.tenant_management_account
  tenant_table_arn          = var.tenant_table_arn
  hostRootDomain            = var.hostRootDomain
  
  providers = {
    aws                = aws.lambdaedge
    aws.lambdaedge     = aws.lambdaedge
  }
}


# CloudFront Distribution for LMS Media
module "cloudfront_distro" {
  source               = "github.com/Cognizant-SPE/lms-terraform-modules//cloudfront-with-s3-origin?ref=v0.5.7.9"
  depends_on           = [module.auth_lambda_module]
  domains              = var.domains
  cf_distribution_name = var.cf_distribution_name
  acm_certificate_arn  = local.acm_certificate_arn_cloudfront
  attach_waf           = local.attach_waf
  web_acl_cloudfront   = local.web_acl_cloudfront
  s3_bucket_name       = var.s3_icons_bucket_name
  check_auth_function_name = var.check_auth_function_name
  configure_failover       = local.cloudfront_configure_failover
  hosted_zone_name         = local.hosted_zone_name
  replica_region           = var.cloudfront_replica_region
  region                   = var.cloudfront_region
  create_route53_entry     = local.create_route53_entry 
  response_headers_policy_id    = var.attach_response_headers_policy ? aws_cloudfront_response_headers_policy.custom_headers_policy.id : null
  attach_response_headers_policy = var.attach_response_headers_policy
  
 providers = {
     aws                = aws.lambdaedge
     aws.replica        = aws.replica-cloudfront
     aws.lambdaedge     = aws.lambdaedge
     aws.sharedservices = aws.sharedservices-cloudfront
   }
}


#-----------
# CF Logging
#-----------

data "aws_iam_policy_document" "cf_distribution_logs_resource_policy" {
  provider = aws.lambdaedge

  statement {
    sid    = "AllowCloudFrontVendedLogsDelivery"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["delivery.logs.amazonaws.com"]
    }

    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = [
      "arn:aws:logs:${var.cloudfront_region}:${data.aws_caller_identity.current.account_id}:log-group:/aws/cloudfront/${var.cf_distribution_name}:*"
    ]
  }
}

resource "aws_cloudwatch_log_resource_policy" "cf_distribution_logs" {
  provider        = aws.lambdaedge
  policy_name     = "cloudfront-standard-logs-${var.cf_distribution_name}"
  policy_document = data.aws_iam_policy_document.cf_distribution_logs_resource_policy.json
}
