region                    = "us-east-1"
failover_region           = "us-west-2"
aws_account_role          = "lms-cross-account-codebuild-role"
interceptor-function-name = "prod-lms-cognito-auth-interceptor-lambda"
configure_failover        = true

defender_layer_name_prefix = "prod"
defender_zipfile           = "/tmp/lms-audit-logs-service/twistlock_defender_layer.zip"
lambda_vpc_name            = "prod-lms-auditlogs-svc-vpc"

lambda-function-name = "prod-lms-user-audit-log-lambda-function"

lambda_alias_name                 = "current-version"
provisioned_concurrent_executions = -1

lambda_function_environment_variables = {
  APP_ENV                                        = "prod"
  REGION_NAME                                    = "us-east-1"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME         = "prod-lms-new-user-audit-log-table"
  AWS_DYNAMODB_POPULARLINKS_AUDIT_LOG_TABLE_NAME = "prod-lms-popularLinks-audit-log-table"
}

dynamo_db_tables = {
  "prod-lms-new-user-audit-log-table" : {
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
  "prod-lms-popularLinks-audit-log-table" : {
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
existing_vpc_id = "vpc-098e3e945a7fe2bdd" 
existing_private_subnets = ["subnet-057d838cced5b82d4", "subnet-00eb9f00b0720d49c", "subnet-009ceb9062c3b2d44"] 
existing_security_group_ids = ["sg-0a9a56a5fa66d2e61"] 
use_existing_vpc_failover = true 
existing_vpc_id_failover = "vpc-034c973446d99e5ac" 
existing_private_subnets_failover = ["subnet-09cad7ea8c2d02ae2", "subnet-02c3b45c7ea0aa5e3", "subnet-0ff7c3c0f743b186c"] 
existing_security_group_ids_failover = ["sg-037ac6b59d621fff7"]