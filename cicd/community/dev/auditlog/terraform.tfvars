region                    = "us-east-1"
failover_region           = "us-west-2"
aws_account_role          = "lms-cross-account-codebuild-role"
interceptor-function-name = "dev-lms-cognito-auth-interceptor-lambda"
configure_failover        = true

defender_layer_name_prefix = "dev"
defender_zipfile           = "/tmp/lms-audit-logs-service/twistlock_defender_layer.zip"
lambda_vpc_name            = "dev-lms-auditlogs-svc-vpc"

lambda-function-name = "dev-lms-user-audit-log-lambda-function"

lambda_alias_name                 = "current-version"
provisioned_concurrent_executions = -1

lambda_function_environment_variables = {
  APP_ENV                                        = "dev"
  REGION_NAME                                    = "us-east-1"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME         = "dev-lms-new-user-audit-log-table"
  AWS_DYNAMODB_POPULARLINKS_AUDIT_LOG_TABLE_NAME = "dev-lms-popularLinks-audit-log-table"
}

dynamo_db_tables = {
  "dev-lms-new-user-audit-log-table" : {
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
  "dev-lms-popularLinks-audit-log-table" : {
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
existing_vpc_id = "vpc-0751a3ca350114cd8" 
existing_private_subnets = ["subnet-05d06e6ebfe32a604", "subnet-08b2be7b4f6e59ae4"] 
existing_security_group_ids = ["sg-0b26dada751c6a9d5"]
 
use_existing_vpc_failover = true 
existing_vpc_id_failover = "vpc-0023a368fb376174f" 
existing_private_subnets_failover = ["subnet-02874a3647eb073ea", "subnet-0e4ebaf3056fb6b0f"] 
existing_security_group_ids_failover = ["sg-0605a00b842a30bcc"]


user-audit-log-lambda-role-name   = "dev-lms-user-audit-log-lambda-role"
user-audit-log-lambda-policy-name = "dev-lms-user-audit-log-lambda-policy"
