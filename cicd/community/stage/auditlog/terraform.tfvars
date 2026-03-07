region                    = "us-east-1"
failover_region           = "us-west-2"
aws_account_role          = "lms-cross-account-codebuild-role"
interceptor-function-name = "stage-lms-cognito-auth-interceptor-lambda"
configure_failover        = true

defender_layer_name_prefix = "stage"
defender_zipfile           = "/tmp/lms-audit-logs-service/twistlock_defender_layer.zip"
lambda_vpc_name            = "stage-lms-auditlogs-svc-vpc"

lambda-function-name = "stage-lms-user-audit-log-lambda-function"

lambda_alias_name                 = "current-version"
provisioned_concurrent_executions = -1

lambda_function_environment_variables = {
  APP_ENV                                        = "stage"
  REGION_NAME                                    = "us-east-1"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME         = "stage-lms-new-user-audit-log-table"
  AWS_DYNAMODB_POPULARLINKS_AUDIT_LOG_TABLE_NAME = "stage-lms-popularLinks-audit-log-table"
}

dynamo_db_tables = {
  "stage-lms-new-user-audit-log-table" : {
    attributes : {
      pk : "S"
      sk : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
  },
  "stage-lms-popularLinks-audit-log-table" : {
    attributes : {
      pk : "S"
      sk : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
  }
}
use_existing_vpc = true 
existing_vpc_id = "vpc-0f0f2ce523bab2d3c" 
existing_private_subnets = ["subnet-03b9d46526309c424", "subnet-0920179024f9b49c6", "subnet-0f8aeb7899d820d32"] 
existing_security_group_ids = ["sg-01220de7cc9893754"] 

use_existing_vpc_failover = true 
existing_vpc_id_failover = "vpc-026472c92f02a44ae" 
existing_private_subnets_failover = ["subnet-0c2ba5182e1e1814f", "subnet-001aa6c60f1bab019", "subnet-04344aa2b3dfbb515"] 
existing_security_group_ids_failover = ["sg-0db4f2bbc43bda696"]