# Microservices related variable values
aws_account_role = "cross-account-codebuild-role"
aws_region       = "ap-south-1"
cloudfront_replica_region = "ap-south-1"
backendregion            = "ap-south-1"

repositoryarn    = "arn:aws:ecr:ap-south-1:061051225269:repository/lms-user-service"
repositoryuri    = "061051225269.dkr.ecr.ap-south-1.amazonaws.com/lms-user-service"

repositoryarn_failover = "arn:aws:ecr:us-east-1:061051225269:repository/lms-user-service"
repositoryuri_failover = "061051225269.dkr.ecr.us-east-1.amazonaws.com/lms-user-service"

api_specfile_path          = "../../../apispec.json"
api_stage_name             = "v1"
apispec_substitutions_file = "../apispec-substitution-stage.json"
api_name_prefix            = "stage-"
api_domain_name            = "userservice.stage-mls.lms.cognizant.com"
api_base_path              = null

tenant_table_name      = "stage-mls-tenant-table"
user_roles_table_name  = "stage-mls-user-roles-table"
user_service_table_name = "stage-mls-user-service-table"
lambda_vpc_name = "stage-user-svc-lambda-mls"

provisioned_concurrent_executions = 4
api_lambda_function_name          = "stage-mls-user-service-function"
lambda_function_memory            = 1024
lambda_function_timeout           = 60
lambda_alias_name                 = "current-version"

#### cloudfront variables ########################################
userpoolregion           = "ap-south-1"
userPoolDomain           = "idp.stage-mls.lms.cognizant.com"
cookieExpirationDays     = 1
cookieDomain             = "stage-mls.lms.cognizant.com"
idTokenExpirationDays    = 0.02083333333
check_auth_function_name = "stage-mls-user-service-media-check-auth"
domains                  = ["usermedia.stage-mls.lms.cognizant.com"]
cf_distribution_name     = "stage-mls-user-management-media"

# Lambda Function Environment Variables
lambda_function_environment_variables = {
  APP_ENV                                       = "stage"
  AWS_DYNAMODB_ENDPOINT                         = "https://dynamodb.ap-south-1.amazonaws.com"
  AWS_DYNAMODB_ROLES_TABLE_NAME                 = "stage-mls-user-roles-table"
  AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME   = "pk"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY      = "createdOn"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER    = "desc"
  AWS_DYNAMODB_USER_TABLE_NAME                  = "stage-mls-user-service-table"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME        = "stage-mls-user-audit-log-table"
  AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME    = "tenantCode"
  AWS_ICONS_S3_BUCKET_NAME                      = "stage-mls-user-service-icons-s3"
  AWS_S3_BUCKET_NAME                            = "stage-mls-user-service-s3"
  REGION_NAME                                   = "ap-south-1"
  THUMBNAIL_BUCKET_REGION_NAME                  = "us-east-1"
  DEFAULT_ROWS_PER_PAGE                         = 10
  LOCAL_STORAGE_PATH                            = "localPath"
  AWS_DYNAMODB_LOGFILE_TABLE_NAME               = "stage-mls-operations-history-table"
  AWS_DYNAMODB_LOGFILE_TABLE_PARTITION_KEY_NAME = "pk"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_KEY   = "createdOn"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_ORDER = "desc"
  AWS_SQS_QUEUE_URL                             = "https://sqs.ap-south-1.amazonaws.com/476348171339/stage-mls-email-service-sqs"
  LMS_ADMIN_EMAIL                               = "skillspringsupport@cognizant.com"
  LMS_FROM_EMAIL_ADDRESS                        = "mylearning@skillspring.cognizant.com"
  LMS_URL                                       = "https://stage-mls.lms.cognizant.com"
  AWS_COGNITO_CERT_URL                          = "https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_sJbMDDVfT/.well-known/jwks.json"
  AWS_COGNITO_ISSUER                            = "https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_sJbMDDVfT"
  CORS_URL                                      = "https://stage-mls.lms.cognizant.com,https://*.stage-mls.lms.cognizant.com"
  COURSE_MANAGEMENT_SERVICE_API_URL             = "https://courseservice.stage-mls.lms.cognizant.com"
  AWS_DYNAMODB_SKILL_LOOKUPS_TABLE_NAME         = "stage-mls-user-skills-table"
  ROLE_ARN                                      = "arn:aws:iam::476348171339:role/cross-account-cognito-role"
  AWS_DYNAMODB_TENANT_TABLE_NAME                = "stage-mls-tenant-table"
  AWS_DYNAMODB_TENANT_CONFIG_TABLE_NAME         = "arn:aws:dynamodb:ap-south-1:336544441798:table/stage-mls-tenant-config-table"
  AWS_COGNITO_DOMAIN                            = "https://idp.stage-mls.lms.cognizant.com"
  AWS_LAMBDA_EXEC_WRAPPER                      = "/opt/datadog_wrapper"
  DD_API_KEY_SECRET_ARN                        = "arn:aws:secretsmanager:ap-south-1:476348171339:secret:Datadog-Keys-0de8efd3-zo9zeD"
  DD_SITE                                      = "datadoghq.com"
  DD_TRACE_OTEL_ENABLED                        = false
  DD_PROFILING_ENABLED                         = false
  DD_SERVERLESS_APPSEC_ENABLED                 = false
  JAVA_TOOL_OPTIONS                            = "-javaagent:/var/task/dd-java-agent.jar"
  DD_TRACE_ENABLED                             = false
  AWS_DYNAMODB_LOOKUP_TABLE_NAME              = "stage-mls-user-lookup-table"
  DD_ENV                                       = "stage"
  DD_INSTRUMENTATION_TELEMETRY_ENABLED           = false
  DD_FLUSH_TO_LOGS                               = false 
  #DD_SERVICE                                     = "userservice.stage-mls.lms.cognizant.com"
  DD_SERVICE                                     = "https://userservice.stage-mls.lms.cognizant.com"
  DD_TAGS                                      = "appid:lms-user-service"
  AWS_DYNAMODB_USER_ACTIVITY_LOG_TABLE_NAME = "stage-mls-user-activity-log-table"
  AWS_USER_MANAGEMENT_EVENT_DETAIL_TYPE        = "UserManagement"
  AWS_USER_MANAGEMENT_EVENT_SOURCE             = "user.status"
  AWS_USER_MANAGEMENT_EVENT_BUS_ARN            = "arn:aws:events:ap-south-1:336544441798:event-bus/stage-mls-user-management-event-bus"
  AWS_DYNAMODB_USER_GLOBAL_SEARCH_HISTORY_TABLE_NAME = "stage-mls-user-global-search-history-table"
  ROOT_DOMAIN_PATH = "stage-mls.lms.cognizant.com"
  DEFAULT_TENANT = "cognizant"
  LOCALE_DATA_TABLE_NAME = "stage-mls-locale-data-table"
  AWS_S3_AI_VOICE_PREVIEW_BUCKET_NAME          = "stage-mls-user-service-s3"
  ALLOWED_DOMAINS = "stage-mls.lms.cognizant.com"
  UI_PREFIXES = "ai,learner-catalog"
}

# DynamodDB Tables
dynamo_db_tables = {
  "stage-mls-user-service-table" : {
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
    replica_regions : [{ region_name : "us-east-1" }]
    stream_enabled: true

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
        projection_type = "ALL"
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
      }
    ]

    # Cross-account Access
    cross_account_role_arns = [
      "arn:aws:iam::476348171339:role/stage-mls-cognito-pretoken-lambda"
    ]
  },
  "stage-mls-tenant-table" : {
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
    replica_regions : [{ region_name : "us-east-1" }]
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
      "arn:aws:iam::476348171339:role/stage-mls-cognito-pretoken-lambda"
    ]
  },
  "stage-mls-user-roles-table" : {
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
  "stage-mls-operations-history-table" : {
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
  "stage-mls-user-skills-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      name : "S"
      type : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    global_secondary_indexes : [
      {
        name : "gsi-Type"
        hash_key : "type"
        range_key : "name"
        projection_type : "ALL"
      }
    ]

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1" }]
  },
  "stage-mls-user-lookup-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      name : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1"}]

    global_secondary_indexes : [
      {
        name : "gsi_sort_name"
        hash_key : "pk"
        range_key : "name"
        projection_type : "ALL"
      }
    ]
  },
  "stage-mls-user-audit-log-table" : {
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
  "stage-mls-user-activity-log-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      timestamp : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1" }]
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
  "stage-mls-user-global-search-history-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      gsiPk : "S"
      gsiSk  : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1"}]

    global_secondary_indexes : [
      {
        name : "gsi_userSearchHistory"
        hash_key : "gsiPk"
        range_key : "gsiSk"
        projection_type : "ALL"
      }
    ]
    },

  "stage-mls-locale-data-table" : {
    attributes : {
      languageCode : "S"
      pageName: "S"
    }

    hash_key : "languageCode"
    range_key : "pageName"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-east-1" }]

  }
  }




# Other additional variables
environment    = "stage"
s3_bucket_name = "stage-mls-user-service-s3"
tenant_identifier = "stage-mls.lms.cognizant.com"
add_item       = "true"

cognito_userpool_arn           = "arn:aws:cognito-idp:ap-south-1:476348171339:userpool/ap-south-1_sJbMDDVfT"
cognito_cross_account_role_arn = "arn:aws:iam::476348171339:role/cross-account-cognito-role"
sqs_queue_arn                  = "arn:aws:sqs:ap-south-1:476348171339:stage-mls-email-service-sqs"
sqs_queue_failover_arn         = "arn:aws:sqs:us-east-1:476348171339:stage-mls-email-service-sqs-failover"
secret_manager_arn             = "arn:aws:secretsmanager:ap-south-1:476348171339:secret:Datadog-Keys-0de8efd3-zo9zeD"
kms_decrypt_arn                = "arn:aws:kms:ap-south-1:476348171339:key/4317fea5-8bac-4109-bfbb-2df8b7a51d3c"
s3_icons_bucket_name = "stage-mls-user-service-icons-s3"

#############################################
### Datadog Forwarder Variables ###
 
enable_datadog_forwarder          = true
enable_datadog_forwarder_failover = true
 
datadog_forwarder_arn          = "arn:aws:lambda:ap-south-1:336544441798:function:datadog-forwarder"
datadog_forwarder_arn_failover = "arn:aws:lambda:us-east-1:336544441798:function:datadog-forwarder"
 
log_retention_in_days = 30

## DynamoDB Trigger configurations ##
trigger_dynamodb_table = "stage-mls-user-service-table"

use_existing_vpc = true
existing_vpc_id = "vpc-07e6493ee7da86f04"
existing_private_subnets = ["subnet-00377df34f6a5a256", "subnet-01be35451429cf259"] 
existing_security_group_ids = ["sg-0b1b59e255d9c2123"]

use_existing_vpc_failover = true
existing_vpc_id_failover = "vpc-03be7b9407f217896"  
existing_private_subnets_failover = ["subnet-0779534319525bee6", "subnet-08b7b20057b7d2b7f"]  
existing_security_group_ids_failover = ["sg-0fbb72eba33e14e53"]

### Event Bus Variables ###
event_bus_name            = "stage-mls-user-service-event-bus"
event_rule_name           = "user-activity-rule"
event_pattern_source      = "aws.cognito-idp"

user_management_event_bus_arn = "arn:aws:events:ap-south-1:336544441798:event-bus/stage-mls-user-management-event-bus"
### Pre-signed URL Upload
CORS_URL         = ["https://stage-mls.lms.cognizant.com", "https://*.stage-mls.lms.cognizant.com"]

tenant_table                 = "stage-mls-tenant-config-table"
tenant_table_arn             = "arn:aws:dynamodb:ap-south-1:336544441798:table/stage-mls-tenant-config-table"
hostRootDomain               = "stage-mls.lms.cognizant.com"

user-audit-log-function-name = "stage-mls-user-audit-log-lambda-function"
user-activity-log-table-name = "stage-mls-user-activity-log-table"
tenant_dynamodb_table = "stage-mls-tenant-table"
attach_response_headers_policy = true

policy_name_for_env = "CustomResponseHeadersPolicy_stage"
lms_credentials = "arn:aws:secretsmanager:ap-south-1:476348171339:secret:cognizant_stage_lms_credentials-d96yjv"
