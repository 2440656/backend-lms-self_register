# Microservices related variable values
aws_account_role = "lms-cross-account-codebuild-role"
aws_region       = "us-east-1"
cloudfront_replica_region = "us-west-2"
repositoryarn    = "arn:aws:ecr:us-east-1:061051225269:repository/lms-user-service"
repositoryuri    = "061051225269.dkr.ecr.us-east-1.amazonaws.com/lms-user-service"

repositoryarn_failover = "arn:aws:ecr:us-west-2:061051225269:repository/lms-user-service"
repositoryuri_failover = "061051225269.dkr.ecr.us-west-2.amazonaws.com/lms-user-service"

api_specfile_path          = "../../../apispec.json"
api_stage_name             = "v1"
apispec_substitutions_file = "../apispec-substitution-prod.json"
api_name_prefix            = "prod-"
api_domain_name            = "userservice.skillspring.cognizant.com"
api_base_path              = null

tenant_table_name         = "prod-lms-tenant-table"
user_roles_table_name     = "prod-lms-user-roles-table"
user_service_table_name   = "prod-lms-user-service-table"
lambda_vpc_name   = "prod-user-svc-lambda-vpc"

provisioned_concurrent_executions = 4
api_lambda_function_name          = "prod-lms-user-service-function"
lambda_function_memory            = 1769
lambda_function_timeout           = 60
lambda_alias_name                 = "current-version"

#### cloudfront variables ########################################
userpoolregion           = "us-east-1"
userPoolDomain           = "idp.skillspring.cognizant.com"
cookieExpirationDays     = 1
cookieDomain             = "skillspring.cognizant.com"
idTokenExpirationDays    = 0.02083333333
check_auth_function_name = "prod-lms-user-service-media-check-auth"
domains                  = ["usermedia.skillspring.cognizant.com"]
cf_distribution_name     = "prod-lms-user-management-media"

#Lambda Function Environment Variables
lambda_function_environment_variables = {
  APP_ENV                                       = "prod" 
  AWS_DYNAMODB_ENDPOINT                         = "https://dynamodb.us-east-1.amazonaws.com"
  AWS_DYNAMODB_ROLES_TABLE_NAME                 = "prod-lms-user-roles-table"
  AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME   = "pk"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY      = "createdOn"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER    = "desc"
  AWS_DYNAMODB_USER_TABLE_NAME                  = "prod-lms-user-service-table"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME        = "prod-lms-user-audit-log-table"
  AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME    = "tenantCode"
  AWS_ICONS_S3_BUCKET_NAME                      = "prod-lms-user-service-icons-s3"
  AWS_S3_BUCKET_NAME                            = "prod-lms-user-service-s3"
  REGION_NAME                                   = "us-east-1"
  THUMBNAIL_BUCKET_REGION_NAME                  = "us-east-1"
  DEFAULT_ROWS_PER_PAGE                         = 10
  LOCAL_STORAGE_PATH                            = "localPath"
  AWS_DYNAMODB_LOGFILE_TABLE_NAME               = "prod-lms-operations-history-table"
  AWS_DYNAMODB_LOGFILE_TABLE_PARTITION_KEY_NAME = "pk"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_KEY   = "createdOn"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_ORDER = "desc"
  AWS_SQS_QUEUE_URL                             = "https://sqs.us-east-1.amazonaws.com/149536458210/prod-lms-email-service-sqs"
  LMS_ADMIN_EMAIL                               = "skillspringsupport@cognizant.com"
  LMS_FROM_EMAIL_ADDRESS                        = "no-reply@skillspring.cognizant.com"
  LMS_URL                                       = "https://skillspring.cognizant.com"
  AWS_COGNITO_CERT_URL                          = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_2S72JvaPZ/.well-known/jwks.json"
  AWS_COGNITO_ISSUER                            = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_2S72JvaPZ" 
  CORS_URL                                      = "https://skillspring.cognizant.com,https://*.skillspring.cognizant.com"
  COURSE_MANAGEMENT_SERVICE_API_URL             = "https://courseservice.skillspring.cognizant.com"  
  AWS_DYNAMODB_SKILL_LOOKUPS_TABLE_NAME         = "prod-lms-user-skills-table"
  ROLE_ARN                                      = "arn:aws:iam::149536458210:role/lms-cross-account-cognito-role"
  AWS_DYNAMODB_TENANT_TABLE_NAME                = "prod-lms-tenant-table"
  AWS_DYNAMODB_TENANT_CONFIG_TABLE_NAME         = "arn:aws:dynamodb:us-east-1:396608790902:table/prod-lms-tenant-config-table"
  AWS_COGNITO_DOMAIN                            = "https://idp.skillspring.cognizant.com"
  AWS_LAMBDA_EXEC_WRAPPER                       = "/opt/datadog_wrapper"
  DD_API_KEY_SECRET_ARN                         = "arn:aws:secretsmanager:us-east-1:149536458210:secret:prod-lms-datadog-secrets-qpMPId"
  DD_SITE                                       = "datadoghq.com"
  DD_TRACE_OTEL_ENABLED                         = false
  DD_PROFILING_ENABLED                          = false
  DD_SERVERLESS_APPSEC_ENABLED                  = false
  JAVA_TOOL_OPTIONS                             = "-javaagent:/var/task/dd-java-agent.jar"
  DD_TRACE_ENABLED                              = false
  AWS_DYNAMODB_LOOKUP_TABLE_NAME               = "prod-lms-user-lookup-table"
  DD_ENV                                        = "PROD"
  DD_INSTRUMENTATION_TELEMETRY_ENABLED           = false
  DD_FLUSH_TO_LOGS                               = false
  DD_SERVICE                                     = "https://userservice.skillspring.cognizant.com"
  DD_TAGS                                       = "appid:lms-user-service"  
  AWS_DYNAMODB_USER_ACTIVITY_LOG_TABLE_NAME = "prod-lms-user-activity-log-table"
  AWS_USER_MANAGEMENT_EVENT_DETAIL_TYPE        = "UserManagement"
  AWS_USER_MANAGEMENT_EVENT_SOURCE             = "user.status"
  AWS_USER_MANAGEMENT_EVENT_BUS_ARN            = "arn:aws:events:us-east-1:396608790902:event-bus/prod-lms-user-management-event-bus"
  AWS_DYNAMODB_USER_GLOBAL_SEARCH_HISTORY_TABLE_NAME = "prod-lms-user-global-search-history-table"
  ROOT_DOMAIN_PATH                              = "skillspring.cognizant.com"
  DEFAULT_TENANT                                = "skillspring"
  LOCALE_DATA_TABLE_NAME                        = "prod-lms-locale-data-table"
  AWS_S3_AI_VOICE_PREVIEW_BUCKET_NAME           = "prod-lms-user-service-s3"
  ALLOWED_DOMAINS = "skillspring.cognizant.com"
  UI_PREFIXES = "ai,learner-catalog"
}

# DynamodDB Tables
dynamo_db_tables = {
  "prod-lms-user-service-table" : {
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
      "arn:aws:iam::149536458210:role/prod-lms-cognito-pretoken-lambda"
    ]
  },
  "prod-lms-tenant-table" : {
      attributes : {
      pk : "S"
      sk : "S"
      tenantIdentifier : "S"
      createdOn : "S"
      type : "S"
      userPoolId : "S"
      updatedDate : "S"
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
        "arn:aws:iam::149536458210:role/prod-lms-cognito-pretoken-lambda"
      ]
    },
  "prod-lms-user-roles-table" : {
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
  "prod-lms-operations-history-table" : {
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
  "prod-lms-user-skills-table" : {
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
  "prod-lms-user-lookup-table" : {
    attributes : {
      pk : "S"
      sk : "S"
      name : "S"
    }

    hash_key : "pk"
    range_key : "sk"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2"}]

    global_secondary_indexes : [
      {
        name : "gsi_sort_name"
        hash_key : "pk"
        range_key : "name"
        projection_type : "ALL"
      }
    ]
  },
  "prod-lms-user-audit-log-table" : {
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
  "prod-lms-user-activity-log-table" : {
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

    global_secondary_indexes : [
      {
        name : "gsi_token_iat"
        hash_key : "pk"
        range_key : "timestamp"
        projection_type : "ALL"
      }
    ]
  },
  "prod-lms-user-global-search-history-table" : {
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
    replica_regions : [{ region_name : "us-west-2"}]

    global_secondary_indexes : [
      {
        name : "gsi_userSearchHistory"
        hash_key : "gsiPk"
        range_key : "gsiSk"
        projection_type : "ALL"
      }
    ]
  },
  "prod-lms-locale-data-table" : {
    attributes : {
      languageCode : "S"
      pageName: "S"
    }

    hash_key : "languageCode"
    range_key : "pageName"

    point_in_time_recovery_enabled : true
    billing_mode : "PAY_PER_REQUEST"
    replica_regions : [{ region_name : "us-west-2" }]
  }
}



# Other additional variables
environment    = "prod"
s3_bucket_name = "prod-lms-user-service-s3"
tenant_identifier         = "skillspring.cognizant.com"
add_item       = "true"

cognito_userpool_arn           = "arn:aws:cognito-idp:us-east-1:149536458210:userpool/us-east-1_2S72JvaPZ"
cognito_cross_account_role_arn = "arn:aws:iam::149536458210:role/lms-cross-account-cognito-role"
sqs_queue_arn                  = "arn:aws:sqs:us-east-1:149536458210:prod-lms-email-service-sqs"
sqs_queue_failover_arn         = "arn:aws:sqs:us-west-2:149536458210:prod-lms-email-service-sqs-failover"
secret_manager_arn             = "arn:aws:secretsmanager:us-east-1:149536458210:secret:prod-lms-datadog-secrets-qpMPId"
kms_decrypt_arn                = "arn:aws:kms:us-east-1:149536458210:key/90f51357-f5fa-439b-b086-21d09d4c0eba"
s3_icons_bucket_name           = "prod-lms-user-service-icons-s3"

#############################################
### Datadog Forwarder Variables ###
 
enable_datadog_forwarder          = false
enable_datadog_forwarder_failover = false
 
datadog_forwarder_arn          = ""
datadog_forwarder_arn_failover = ""
 
log_retention_in_days = 30

## DynamoDB Trigger configurations ##
trigger_dynamodb_table = "prod-lms-user-service-table"

### Event Bus Variables ###
event_bus_name            = "prod-lms-user-service-event-bus"
event_rule_name           = "user-activity-rule"
event_pattern_source      = "aws.cognito-idp"

user_management_event_bus_arn = "arn:aws:events:us-east-1:396608790902:event-bus/prod-lms-user-management-event-bus"

########################Adding commnon vpc variables##########################
use_existing_vpc = true 
existing_vpc_id = "vpc-098e3e945a7fe2bdd" 
existing_private_subnets = ["subnet-057d838cced5b82d4", "subnet-00eb9f00b0720d49c", "subnet-009ceb9062c3b2d44"] 
existing_security_group_ids = ["sg-0a9a56a5fa66d2e61"] 
use_existing_vpc_failover = true 
existing_vpc_id_failover = "vpc-034c973446d99e5ac" 
existing_private_subnets_failover = ["subnet-09cad7ea8c2d02ae2", "subnet-02c3b45c7ea0aa5e3", "subnet-0ff7c3c0f743b186c"] 
existing_security_group_ids_failover = ["sg-037ac6b59d621fff7"]
### Pre-signed URL Upload
CORS_URL         = ["https://skillspring.cognizant.com", "https://*.skillspring.cognizant.com"]

tenant_table                 = "prod-lms-tenant-config-table"
tenant_table_arn             = "arn:aws:dynamodb:us-east-1:396608790902:table/prod-lms-tenant-config-table"
hostRootDomain               = "skillspring.cognizant.com"

user-audit-log-function-name = "prod-lms-user-audit-log-lambda-function"
user-activity-log-table-name = "prod-lms-user-activity-log-table"
tenant_dynamodb_table =        "prod-lms-tenant-table"
attach_response_headers_policy = true

policy_name_for_env = "CustomResponseHeadersPolicy_prod"
lms_credentials = "arn:aws:secretsmanager:us-east-1:149536458210:secret:community_prod_lms_credentials-9cqZf9"
