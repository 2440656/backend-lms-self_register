# Microservices related variable values
aws_account_role          = "cross-account-codebuild-role"
aws_region                = "us-east-1"
cloudfront_replica_region = "us-west-2"

repositoryarn = "arn:aws:ecr:us-east-1:061051225269:repository/lms-user-service"
repositoryuri = "061051225269.dkr.ecr.us-east-1.amazonaws.com/lms-user-service"

api_specfile_path          = "../sbx-apispec.json"
api_stage_name             = "v1"
apispec_substitutions_file = "../apispec-substitution-sbx.json"
api_name_prefix            = "sbx-"
api_domain_name            = "userservice.sbx.lms.cognizant.com"
api_base_path              = null

lambda_vpc_name         = "sbx-user-svc-lambda-vpc"
tenant_table_name       = "sbx-lms-tenant-table"
user_roles_table_name   = "sbx-lms-user-roles-table"
user_service_table_name = "sbx-lms-user-service-table"

provisioned_concurrent_executions = -1
api_lambda_function_name          = "sbx-lms-user-service-function"
lambda_function_memory            = 1024
lambda_function_timeout           = 60
lambda_alias_name                 = "current-version"

#### cloudfront variables ########################################
userpoolregion           = "us-east-1"
userPoolDomain           = "idp2.sbx.lms.cognizant.com"
cookieExpirationDays     = 1
cookieDomain             = "sbx.lms.cognizant.com"
idTokenExpirationDays    = 0.02083333333
check_auth_function_name = "sbx-lms-user-service-media-check-auth"
domains                  = ["usermedia.sbx.lms.cognizant.com"]
cf_distribution_name     = "sbx-lms-user-management-media"

#Lambda Function Environment Variables
lambda_function_environment_variables = {
  APP_ENV                                       = "sbx"
  AWS_DYNAMODB_ENDPOINT                         = "https://dynamodb.us-east-1.amazonaws.com"
  AWS_DYNAMODB_ROLES_TABLE_NAME                 = "sbx-lms-user-roles-table"
  AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME   = "pk"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY      = "createdOn"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER    = "desc"
  AWS_DYNAMODB_USER_TABLE_NAME                  = "sbx-lms-user-service-table"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME        = "sbx-lms-user-audit-log-table"
  AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME    = "tenantCode"
  AWS_ICONS_S3_BUCKET_NAME                      = "sbx-lms-user-service-icons-s3"
  AWS_S3_BUCKET_NAME                            = "sbx-lms-user-service-s3"
  REGION_NAME                                   = "us-east-1"
  THUMBNAIL_BUCKET_REGION_NAME                  = "us-east-1"
  DEFAULT_ROWS_PER_PAGE                         = 10
  LOCAL_STORAGE_PATH                            = "localPath"
  AWS_DYNAMODB_LOGFILE_TABLE_NAME               = "sbx-lms-operations-history-table"
  AWS_DYNAMODB_LOGFILE_TABLE_PARTITION_KEY_NAME = "pk"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_KEY   = "createdOn"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_ORDER = "desc"
  AWS_SQS_QUEUE_URL                             = "https://sqs.us-east-1.amazonaws.com/885657071929/sbx-lms-email-service-sqs"
  LMS_ADMIN_EMAIL                               = "skillspringsupport@cognizant.com"
  LMS_FROM_EMAIL_ADDRESS                        = "no-reply@cognizantproducts.com"
  LMS_URL                                       = "https://sbx.lms.cognizant.com"
  AWS_COGNITO_CERT_URL                          = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_wj8qsRWYQ/.well-known/jwks.json"
  AWS_COGNITO_ISSUER                            = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_wj8qsRWYQ"
  CORS_URL                                      = "https://sbx.lms.cognizant.com,https://*.sbx.lms.cognizant.com,http://localhost:3000"
  COURSE_MANAGEMENT_SERVICE_API_URL             = "https://courseservice.sbx.lms.cognizant.com"
  AWS_DYNAMODB_SKILL_LOOKUPS_TABLE_NAME         = "sbx-lms-user-skills-table"
  ROLE_ARN                                      = "arn:aws:iam::885657071929:role/cross-account-cognito-role"
  AWS_DYNAMODB_TENANT_TABLE_NAME                = "sbx-lms-tenant-table"
  AWS_DYNAMODB_TENANT_CONFIG_TABLE_NAME         = "arn:aws:dynamodb:us-east-1:885657071929:table/sbx-lms-tenant-config-table"
  AWS_COGNITO_DOMAIN                            = "https://idp2.sbx.lms.cognizant.com"
  AWS_DYNAMODB_LOOKUP_TABLE_NAME                = "sbx-lms-user-lookup-table"
  DD_ENV                                        = "sbx"
  DD_FLUSH_TO_LOGS                              = false
  DD_INSTRUMENTATION_TELEMETRY_ENABLED          = false
  DD_TAGS                                       = "appid:lms-user-service"
  #DD_SERVICE                                   = "userservice.sbx.lms.cognizant.com" 
  DD_SERVICE                                   = "https://userservice.sbx.lms.cognizant.com"
  AWS_DYNAMODB_USER_ACTIVITY_LOG_TABLE_NAME    = "sbx-lms-user-activity-log-table"
  AWS_USER_MANAGEMENT_EVENT_DETAIL_TYPE        = "UserManagement"
  AWS_USER_MANAGEMENT_EVENT_SOURCE             = "user.status"
  AWS_USER_MANAGEMENT_EVENT_BUS_ARN            = "arn:aws:events:us-east-1:885657071929:event-bus/sbx-lms-user-management-event-bus"
  AWS_DYNAMODB_USER_GLOBAL_SEARCH_HISTORY_TABLE_NAME = "sbx-lms-user-global-search-history-table"
  AWS_S3_AI_VOICE_PREVIEW_BUCKET_NAME          = "sbx-lms-user-service-s3"
  ROOT_DOMAIN_PATH = "sbx.lms.cognizant.com"
  DEFAULT_TENANT = "t-2"
  LOCALE_DATA_TABLE_NAME = "sbx-lms-locale-data-table"
  ALLOWED_DOMAINS = "sbx.lms.cognizant.com,localhost"
  UI_PREFIXES = "ai,learner-catalog"
}

# DynamodDB Tables
dynamo_db_tables = {
  "sbx-lms-user-service-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      tenantCode : "S"
      createdOn : "S"
      gsiSortFNLN : "S"
      status : "S"
      userType : "S"
      userAccountExpiryDate : "S"
      country : "S"
      emailId : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
    stream_enabled : true

    global_secondary_indexes : [
      {
        name : "gsi_sort_createdOn"
        hash_key : "tenantCode"
        range_key : "createdOn"
        projection_type : "ALL"
      },
      {
        name : "gsi_sort_name"
        hash_key : "tenantCode"
        range_key : "gsiSortFNLN"
        projection_type : "ALL"
      },
      {
        name : "gsi_sort_status"
        hash_key : "tenantCode"
        range_key : "status"
        projection_type : "ALL"
      },
      {
        name : "gsi_sort_userType"
        hash_key : "tenantCode"
        range_key : "userType"
        projection_type : "ALL"
      },
      {
        name : "gsi_sort_expiryDate"
        hash_key : "tenantCode"
        range_key : "userAccountExpiryDate"
        projection_type : "ALL"
      },
      {
        name : "gsi_sort_country"
        hash_key : "tenantCode"
        range_key : "country"
        projection_type : "ALL"
      },
      {
        name : "gsi_sort_emailId"
        hash_key : "emailId"
        range_key : "createdOn"
        projection_type : "ALL"
      },
      {
        name : "gsi_emailId_sk"
        hash_key : "emailId"
        range_key : "sk"
        projection_type : "ALL"
      }
    ]

    # Cross-account Access
    cross_account_role_arns = [
      "arn:aws:iam::885657071929:role/sbx-lms-cognito-pretoken-lambda"
    ]
  },
  "sbx-lms-tenant-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      tenantIdentifier : "S"
      createdOn : "S"
      type : "S"
      userPoolId : "S"
      updatedDate : "S",
      startDate : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
    stream_enabled : true

    global_secondary_indexes : [
      {
        name : "gsi_sort_createdOn"
        hash_key : "tenantIdentifier"
        range_key : "createdOn"
        projection_type : "ALL"
      },
      {
        name : "gsi_type"
        hash_key : "type"
        range_key : "sk"
        projection_type : "ALL"
      },
      {
        name : "gsi_banner_type"
        hash_key : "pk"
        range_key : "startDate"
        projection_type : "ALL"
      },
      {
        name : "gsi_userPoolId"
        hash_key : "userPoolId"
        range_key : "sk"
        projection_type : "ALL"
      },
      {
        name : "gsi_type_updatedDate"
        hash_key : "type"
        range_key : "updatedDate"
        projection_type : "ALL"
      }
    ]

    # Cross-account Access
    cross_account_role_arns = [
      "arn:aws:iam::885657071929:role/sbx-lms-cognito-pretoken-lambda"
    ]
  },
  "sbx-lms-user-roles-table" : {
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
  "sbx-lms-operations-history-table" : {
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
  "sbx-lms-user-skills-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      name : "S"
      type : "S"
      tenantType : "S"
      normalizedName : "S"
      normalizedCode : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    global_secondary_indexes : [
      {
        name : "gsi-Type"
        hash_key : "type"
        range_key : "name"
        projection_type : "ALL"
      },
      {
        name : "gsi_skillName"
        hash_key : "tenantType"
        range_key : "normalizedName"
        projection_type : "ALL"
      },
      {
        name : "gsi_skillCode"
        hash_key : "tenantType"
        range_key : "normalizedCode"
        projection_type : "ALL"
      }
    ]

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
  },
  "sbx-lms-user-lookup-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      name : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]

    global_secondary_indexes : [
      {
        name : "gsi_sort_name"
        hash_key : "pk"
        range_key : "name"
        projection_type : "ALL"
      }
    ]
  },
  "sbx-lms-user-audit-log-table" : {
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
  "sbx-lms-user-activity-log-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      timestamp : "S"
    }
  
    hash_key : "pk"
    range_key : "sk"
  
    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
    stream_enabled : true
  
    global_secondary_indexes : [
      {
        name : "gsi_token_iat"
        hash_key : "pk"
        range_key : "timestamp"
        projection_type : "ALL"
      }
    ]
  },
  "sbx-lms-user-global-search-history-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      gsiPk : "S"
      gsiSk : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]

    global_secondary_indexes : [
      {
        name : "gsi_userSearchHistory"
        hash_key : "gsiPk"
        range_key : "gsiSk"
        projection_type : "ALL"
      }
    ]
  },
  "sbx-lms-locale-data-table" : {
    attributes : {
      languageCode : "S"
      pageName : "S"
    }

    hash_key : "languageCode"
    range_key : "pageName"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
  }
}


# Other additional variables
environment       = "sbx"
s3_bucket_name    = "sbx-lms-user-service-s3"
tenant_identifier = "sbx.lms.cognizant.com"
add_item          = "true"

cognito_userpool_arn           = "arn:aws:cognito-idp:us-east-1:885657071929:userpool/*"
cognito_cross_account_role_arn = "arn:aws:iam::885657071929:role/cross-account-cognito-role"
sqs_queue_arn                  = "arn:aws:sqs:us-east-1:885657071929:sbx-lms-email-service-sqs"
s3_icons_bucket_name           = "sbx-lms-user-service-icons-s3"

enable_lambda_tracing    = true
xray_tracing_lambda      = "PassThrough"
xray_tracing_enabled_api = false

## DynamoDB Trigger configurations ##
trigger_dynamodb_table = "sbx-lms-user-service-table"

use_existing_vpc            = true
existing_vpc_id             = "vpc-0a9c92f523de11e07"
existing_private_subnets    = ["subnet-066328a763c740e0f", "subnet-03584fc1c5f894fb9", "subnet-071d3fabece8c5e5f"]
existing_security_group_ids = ["sg-0ac5ea9c14a9510de"]

### Event Bus Variables ###
event_bus_name       = "sbx-lms-user-service-event-bus"
event_rule_name      = "user-activity-rule"
event_pattern_source = "aws.cognito-idp"

user_management_event_bus_arn = "arn:aws:events:us-east-1:885657071929:event-bus/sbx-lms-user-management-event-bus"
### Pre-signed URL Upload
CORS_URL         = ["https://sbx.lms.cognizant.com", "https://*.sbx.lms.cognizant.com", "http://localhost:3000"]

tenant_table     = "sbx-lms-tenant-config-table"
tenant_table_arn = "arn:aws:dynamodb:us-east-1:885657071929:table/sbx-lms-tenant-config-table"
hostRootDomain   = "sbx.lms.cognizant.com"

# user-audit-log-function-name = "sbx-lms-user-audit-log-lambda-function"
# user-activity-log-table-name = "sbx-lms-user-activity-log-table"
tenant_dynamodb_table          = "sbx-lms-tenant-table"
attach_response_headers_policy = true

policy_name_for_env = "CustomResponseHeadersPolicy_sbx_user"
lms_credentials     = "arn:aws:secretsmanager:us-east-1:885657071929:secret:community_sbx_lms_credentials-UcXFGE"

############################## ECS backend variables ############################

ecs_service_name    = "sbx-lms-user-service"

ecs_cluster_id      = "arn:aws:ecs:us-east-1:885657071929:cluster/sbx1-lms-ecs-cluster"
alb_arn             = "arn:aws:elasticloadbalancing:us-east-1:885657071929:loadbalancer/app/sbx1-lms-ecs-cluster/4512402bb2d35fd1"
execution_role_arn  = "arn:aws:iam::885657071929:role/sbx1-lms-ecs-cluster-execution-role"

lb_listner_port  = 8080
container_port   = 8080

# Desired tasks and sizing
ecs_desired_container_count = 1
ecs_container_cpu           = 512
ecs_container_memory        = 1024

health_check_path = "/actuator/health"



existing_vpc_id_1             = "vpc-03be7b9407f217896"
existing_private_subnets_1    = ["subnet-0779534319525bee6", "subnet-08b7b20057b7d2b7f"]
existing_security_group_ids_1 = ["sg-0e6b599cb0902ca78"]

