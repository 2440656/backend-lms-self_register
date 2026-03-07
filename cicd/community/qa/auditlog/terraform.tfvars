region                    = "us-east-1"
failover_region           = "us-west-2"
aws_account_role          = "lms-cross-account-codebuild-role"
interceptor-function-name = "qa-lms-cognito-auth-interceptor-lambda"
configure_failover        = true

defender_layer_name_prefix = "qa"
defender_zipfile           = "/tmp/lms-audit-logs-service/twistlock_defender_layer.zip"
lambda_vpc_name            = "qa-lms-auditlogs-svc-vpc"

lambda-function-name = "qa-lms-user-audit-log-lambda-function"

lambda_alias_name                 = "current-version"
provisioned_concurrent_executions = -1

lambda_function_environment_variables = {
  APP_ENV                                        = "qa"
  REGION_NAME                                    = "us-east-1"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME         = "qa-lms-new-user-audit-log-table"
  AWS_DYNAMODB_POPULARLINKS_AUDIT_LOG_TABLE_NAME = "qa-lms-popularLinks-audit-log-table"
}

dynamo_db_tables = {
  "qa-lms-new-user-audit-log-table" : {
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
  "qa-lms-popularLinks-audit-log-table" : {
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
use_existing_vpc            = true
existing_vpc_id             = "vpc-05118d2bf72688438"
existing_private_subnets    = ["subnet-0900a079939ad759e", "subnet-01389e5a828cd52d0", "subnet-0f65f62e2311e49ab"]
existing_security_group_ids = ["sg-0f71f1bbdbb581872"]

use_existing_vpc_failover            = true
existing_vpc_id_failover             = "vpc-086bc78ad568b8987"
existing_private_subnets_failover    = ["subnet-0f213b15d23d0dc7b", "subnet-0ae4d0aa15b3b1aae", "subnet-0dce4d1946c44592e"]
existing_security_group_ids_failover = ["sg-033d5b27ca0cad10c"]
