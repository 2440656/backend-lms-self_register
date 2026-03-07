region                    = "ap-south-1"
failover_region           = "us-east-1"
aws_account_role          = "cross-account-codebuild-role"
interceptor-function-name = "stage-mls-cognito-auth-interceptor-lambda"
configure_failover        = true

defender_layer_name_prefix = "stage"
defender_zipfile           = "/tmp/lms-audit-logs-service/twistlock_defender_layer.zip"
lambda_vpc_name            = "stage-mls-auditlogs-svc-vpc"

lambda-function-name = "stage-mls-user-audit-log-lambda-function"

lambda_alias_name                 = "current-version"
provisioned_concurrent_executions = -1

lambda_function_environment_variables = {
  APP_ENV                                        = "stage"
  REGION_NAME                                    = "ap-south-1"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME         = "stage-mls-new-user-audit-log-table"
  AWS_DYNAMODB_POPULARLINKS_AUDIT_LOG_TABLE_NAME = "stage-mls-popularLinks-audit-log-table"
}

dynamo_db_tables = {
  "stage-mls-new-user-audit-log-table" : {
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
  "stage-mls-popularLinks-audit-log-table" : {
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
existing_vpc_id = "vpc-07e6493ee7da86f04"
existing_private_subnets = ["subnet-00377df34f6a5a256", "subnet-01be35451429cf259"] 
existing_security_group_ids = ["sg-0b1b59e255d9c2123"]
use_existing_vpc_failover = true
existing_vpc_id_failover = "vpc-03be7b9407f217896"  
existing_private_subnets_failover = ["subnet-0779534319525bee6", "subnet-08b7b20057b7d2b7f"]  
existing_security_group_ids_failover = ["sg-0fbb72eba33e14e53"]