variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
}

variable "aws_shared_services_account_id" {
  description = "AWS Shared Services Account ID"
  type        = string
}

variable "aws_account_role" {
  description = "IAM Role Name in the AWS Account"
  type        = string
}

variable "aws_region" {
  default = "us-east-1"
}

variable "aws_failover_region" {
  default = "us-west-2"
}

variable "repositoryuri" {
  description = "The ECR repository URI containing the function's deployment package"
  type        = string
}

variable "imageversion" {
  description = "The image version containing the function's deployment package"
  type        = string
}

variable "repositoryarn" {
  description = "ARN of ECR repository containing the function's deployment package"
  type        = string
}

variable "repositoryuri_failover" {
  description = "The ECR repository URI containing the function's deployment package during failover"
  type        = string
}

variable "repositoryarn_failover" {
  description = "ARN of ECR repository containing the function's deployment package during failover"
  type        = string
}

variable "environment" {
  description = "Environment - dev, qa, stage, prod"
  type        = string
}

variable "s3_bucket_name" {
  description = "S3 Bucket Name"
  type        = string
}

variable "api_specfile_path" {
  description = "Path of apispec file"
  type        = string
}

variable "apispec_substitutions_file" {
  type        = string
  default     = ""
  description = "The relative or absolute path of the JSON file with substitutions to be made to APISpec"
}

variable "api_name_prefix" {
  type        = string
  description = "Optional variable to allow the API name to be prefixed with the specific value. The provided value will be prefixed to the value under info.title in the API spec. This allows the same spec to be used across environments"
  default     = ""
}

variable "api_stage_name" {
  description = "API Stage Name"
  type        = string
}

variable "api_base_path" {
  description = "API Base Path"
  type        = string
}

variable "api_domain_name" {
  description = "Domain Name for the API"
  type        = string
}

variable "api_lambda_function_name" {
  description = "API Lambda Fucntion Name"
  type        = string
}

variable "provisioned_concurrent_executions" {
  type        = number
  description = "Amount of capacity to allocate. Set to 1 or greater to enable, or set to -1 to disable provisioned concurrency."
  default     = -1
}

variable "lambda_alias_name" {
description = "Alias name for Lambda"
default = "current-version"
}

variable "lambda_function_environment_variables" {
  type        = map(string)
  description = "A map that defines environment variables for the Lambda Function"
  default     = {}
}


variable "lambda_function_memory" {
  type        = number
  description = "Amount of memory in MB Lambda Function can use at runtime"
  default     = 128
}

variable "lambda_function_timeout" {
  type        = number
  description = "Amount of time Lambda Function has to run in seconds"
  default     = 30
}

variable "lambda_vpc_name" {
  type        = string
  description = "Name of the VPC for Lambda"
}

variable "dynamo_db_tables" {
  type = map(object({
    attributes   = optional(map(string), {})
    hash_key     = optional(string, null)
    range_key    = optional(string, null)
    billing_mode = optional(string, "PAY_PER_REQUEST")

    read_capacity  = optional(number, null)
    write_capacity = optional(number, null)

    point_in_time_recovery_enabled = optional(bool, false)
    ttl_enabled                    = optional(bool, false)
    ttl_attribute_name             = optional(string, "")
    stream_enabled                 = optional(bool, false)
    stream_view_type               = optional(string, null)
    table_class                    = optional(string, null)
    deletion_protection_enabled    = optional(bool, false)

    replica_regions = optional(any, [])

    global_secondary_indexes = optional(any, [])
    local_secondary_indexes  = optional(any, [])
    import_table             = optional(any, {})

    cross_account_role_arns  = optional(list(string), [])

  }))
  description = "The dynamo db tables configurations"
  default     = {}
}


########### Variables required for additional IAM Roles #####
variable "sqs_queue_arn" {
  description = "SQS Queue ARN"
  type        = string
}

variable "secret_manager_arn" {
  description = "Secret Manager ARN"
  type        = string
}

variable "kms_decrypt_arn" {
  description = "KMS Decrypt"
  type        = string
}

variable "sqs_queue_failover_arn" {
  description = "SQS Queue Failover ARN"
  type        = string
}

variable "cognito_userpool_arn" {
  description = "Cognito UserPool ARN"
  type        = string
}

variable "cognito_cross_account_role_arn" {
  description = "Cognito Cross Account Role ARN"
  type        = string
}

variable "tenant_identifier" {
  description = "Tenant Identifier"
  type        = string
}

variable "add_item" {
  description = "Flag to determine if DynamoDB Table Items should be added"
  type        = bool
}

#########################################################################

###### adding Datadog variables v0.5.6.5################################
variable "enable_datadog_forwarder" {
  type        = bool
  description = "Flag for Datadog Forwarder Subscription to the Lambda Log Group"
  default     = false
}
 
variable "enable_datadog_forwarder_failover" {
  type        = bool
  description = "Flag for Datadog Forwarder Subscription to the Lambda Log Group"
  default     = false
}
 
variable "datadog_forwarder_arn" {
  type        = string
  description = "ARN of the Datadog Forwarder Lambda"
  default     = ""
}
 
variable "datadog_forwarder_arn_failover" {
  type        = string
  description = "ARN of the Datadog Forwarder Lambda"
  default     = ""
}
 
variable "log_retention_in_days" {
  description = "Retention days for Logs"
  type        = number
  default     = 30
}



### Variable for Trigger ###

variable "trigger_dynamodb_table" {
  description = "DynamoDB Table to trigger the Lambda"
  type        = string
}

variable "tenant_dynamodb_table" {
  description = "DynamoDB Table to trigger the Lambda"
  type        = string
}

##### Variables for existing VPC in Primary Region #####

variable "use_existing_vpc" {
  description = "Flag to indicate whether to use an existing VPC or create a new one"
  type        = bool
  default     = false
}

variable "existing_vpc_id" {
  description = "ID of the existing VPC to use if use_existing_vpc is true"
  type        = string
  default     = ""
}

variable "existing_private_subnets" {
  description = "List of private subnets from the existing VPC"
  type        = list(string)
  default     = []
}

variable "existing_security_group_ids" {
  description = "Security groups to attach to Lambda if using existing VPC"
  type        = list(string)
  default     = []
}

### Variables for existing VPC in Failover Region ###

variable "use_existing_vpc_failover" {
  description = "Flag to indicate whether to use an existing VPC for failover region"
  type        = bool
  default     = false
}

variable "existing_vpc_id_failover" {
  description = "ID of the existing VPC in failover region"
  type        = string
  default     = ""
}

variable "existing_private_subnets_failover" {
  description = "List of private subnets in failover region's existing VPC"
  type        = list(string)
  default     = []
}

variable "existing_security_group_ids_failover" {
  description = "Security groups to attach to Lambda in failover region"
  type        = list(string)
  default     = []
}

### Variables for Event Bus ###

variable "event_bus_name" {
  description = "The name of the custom event bus"
  type        = string
}

variable "event_rule_name" {
  description = "The name of the rule to attach to the event bus"
  type        = string
}

variable "event_pattern_source" {
  description = "The source for the event pattern"
  type        = string
}

# event bus ARN from User Management account
variable "user_management_event_bus_arn" {
  description = "ARN of the Course Management listener Event Bus"
  type        = string
}

variable "aws_replica_region" {
  default = "us-west-2"
}
### Pre-signed URL Upload
variable "CORS_URL" {
  description = "Name of the S3 Bucket"
  type        = list(string)
}

variable "enable_lambda_tracing" {
  type = bool
  description = "Enable or disable Lambda X-Ray tracing"
  default = true
}

variable "xray_tracing_lambda" {
  type = string
  description = "Enable X-Ray Tracing for Lambda"
  default = "Active"
}
 
variable "xray_tracing_enabled_api" {
  type = bool
  description = "Enable X-Ray Tracing for API"
  default = true
}

###### adding s3_icons bucket with cloud front access #########
variable "s3_icons_bucket_name" {
  description = "S3 Bucket Name for icons"
  type        = string
}
variable "userpoolregion" {
  description = "User Pool Region"
  type        = string
  default     = "us-east-1"
}

variable "userPoolDomain" {
  description = "User Pool Domain"
  type        = string
}

variable "cookieExpirationDays" {
  description = "Cookie Expiration Days"
  type        = number
  default     = 1
}

variable "cookieDomain" {
  description = "Cookie Domain"
  type        = string
}

variable "idTokenExpirationDays" {
  description = "ID Token Expiration Days"
  type        = number
}

variable "domains" {
  description = "List of domains"
  type        = list(string)
  default     = []
}

variable "cf_distribution_name" {
  description = "CloudFront Distribution Name"
  type        = string
}

variable "check_auth_function_name" {
  description = "Check Auth Function Name"
  type        = string
}

variable "cloudfront_region" {
  description = "Primary Region for CloudFront"
  default = "us-east-1"
}

variable "cloudfront_replica_region" {
  description = "Replica Region for CloudFront"
}

variable "user-audit-log-function-name" {
    description = "User Audit Log Lambda Function Name"
    type        = string
}

variable "user-activity-log-table-name" {
    description = "DynamoDB Table Name for User Activity Log"
    type        = string
}

variable "policy_name_for_env" {
  description = "Policy name for env."
  type        = string
}
variable "attach_response_headers_policy" {
  description = "Flag to determine whether to attach the response headers policy."
  type        = bool
  default     = false
}
variable "tenant_management_account" {
  description = "Tenant Management AWS Account ID"
  type        = string
}

variable "tenant_table" {
  description = "Tenant DynamoDB Table Name"
  type        = string
}

variable "tenant_table_arn" {
  type        = string
  description = "The ARN of the tenant table in DynamoDB"
}

variable "hostRootDomain" {
  type        = string
  description = "The root domain for the host"
}

variable "lms_credentials" {
  description = "Secret Manager ARN for Community dev LMS Credentials"
}

# DynamoDB Table Names
variable "tenant_table_name" {
  description = "Tenant DynamoDB Table Name"
  type        = string
  
}

variable "user_roles_table_name" {
  description = "User Roles DynamoDB Table Name"
  type        = string
  
}

variable "user_service_table_name" {
  description = "User Service DynamoDB Table Name"
  type        = string
  
}