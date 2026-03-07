variable "region" {
  default = "ap-south-1"
}

variable "failover_region" {
  default = "us-east-1"
}

variable "configure_failover" {
  default = false
}

variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
}

variable "aws_account_role" {
  description = "IAM Role Name in the AWS Account"
  type        = string
}

variable "interceptor-function-name" {
  type        = string
  description = "Name for the Lambda function"
}

variable "defender_zipfile" {
  type        = string
  description = "The path to the defender package"
}

variable "defender_layer_name_prefix" {
  type        = string
  description = "Prefix if any to associate with layer name"
  default     = ""
}

variable "lambda_vpc_name" {
  type        = string
  description = "Name of the VPC for Lambda"
}

variable "lambda_alias_name" {
  description = "Alias name for Lambda"
  default     = "current-version"
}

variable "provisioned_concurrent_executions" {
  type        = number
  description = "Amount of capacity to allocate. Set to 1 or greater to enable, or set to -1 to disable provisioned concurrency."
  default     = -1
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

  }))
  description = "The dynamo db tables configurations"
  default     = {}
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

variable "lambda_enable_vpc" {
  type        = bool
  description = "Create VPC and create Lambda function in VPC"
  default     = true
}

### Adding xray tracing variable  ####

variable "enable_lambda_tracing" {
  type        = bool
  description = "Enable Lambda Tracing"
  default     = true
}

variable "xray_tracing_lambda" {
  type        = string
  description = "Enable X-Ray Tracing for Lambda"
  default     = "Active"
}

### Variables for user audit log ###

variable "lambda-function-name" {
  type        = string
  description = "Name for the email Audit Log Lambda function"
}

variable "lambda_function_environment_variables" {
  type        = map(string)
  description = "A map that defines environment variables for the Lambda Function"
  default     = {}
}