aws_account_role          = "cross-account-codebuild-role"
region                    = "ap-south-1"
failover_region           = "us-east-1"
interceptor-function-name = "prod-mls-cognito-auth-interceptor-lambda"
configure_failover        = true

defender_layer_name_prefix = "prod"
defender_zipfile           = "/tmp/lms-audit-logs-service/twistlock_defender_layer.zip"
lambda_vpc_name            = "prod-mls-auditlogs-svc-vpc"

lambda-function-name = "prod-mls-user-audit-log-lambda-function"

lambda_alias_name                 = "current-version"
provisioned_concurrent_executions = -1

lambda_function_environment_variables = {
  APP_ENV                                        = "prod"
  REGION_NAME                                    = "ap-south-1"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME         = "prod-mls-new-user-audit-log-table"
  AWS_DYNAMODB_POPULARLINKS_AUDIT_LOG_TABLE_NAME = "prod-mls-popularLinks-audit-log-table"
}

dynamo_db_tables = {
  "prod-mls-new-user-audit-log-table" : {
    attributes : {
      pk : "S"
      sk : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1" }]
  },
  "prod-mls-popularLinks-audit-log-table" : {
    attributes : {
      pk : "S"
      sk : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1" }]
  }
}
use_existing_vpc = true  
existing_vpc_id = "vpc-04bc2d7896738b008"
existing_private_subnets = ["subnet-0ee5cb04fce1da29e", "subnet-0fe0d9adebe4098b9"]
existing_security_group_ids = ["sg-089740091682108ad"]
use_existing_vpc_failover = true
existing_vpc_id_failover = "vpc-0e23c5da3cf65aa1a"
existing_private_subnets_failover = ["subnet-0868368d38982fba3", "subnet-0a9f340ff535aef20"]
existing_security_group_ids_failover = ["sg-0302487ed79ee853d"]
