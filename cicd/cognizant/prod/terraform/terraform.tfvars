# Microservices related variable values
aws_account_role = "cross-account-codebuild-role"
aws_region       = "ap-south-1"
cloudfront_replica_region = "ap-south-1"
backendregion    = "ap-south-1"
repositoryarn    = "arn:aws:ecr:ap-south-1:061051225269:repository/lms-user-service"
repositoryuri    = "061051225269.dkr.ecr.ap-south-1.amazonaws.com/lms-user-service"

repositoryarn_failover = "arn:aws:ecr:us-east-1:061051225269:repository/lms-user-service"
repositoryuri_failover = "061051225269.dkr.ecr.us-east-1.amazonaws.com/lms-user-service"



api_specfile_path          = "../../../apispec.json"
api_stage_name             = "v1"
apispec_substitutions_file = "../apispec-substitution-prod.json"
api_name_prefix            = "prod-"
api_domain_name            = "userservice.myskillspring.cognizant.com"
api_base_path              = null

tenant_table_name         = "prod-mls-tenant-table"
user_roles_table_name     = "prod-mls-user-roles-table"
user_service_table_name   = "prod-mls-user-service-table"
lambda_vpc_name           = "prod-user-svc-lambda-mls"

provisioned_concurrent_executions = 4
api_lambda_function_name          = "prod-mls-user-service-function"
lambda_function_memory            = 1769
lambda_function_timeout           = 60
lambda_alias_name                 = "current-version"



# cloudfront variables
userpoolregion           = "ap-south-1"
userPoolDomain           = "idp.myskillspring.cognizant.com"
cookieExpirationDays     = 1
cookieDomain             = "myskillspring.cognizant.com"
idTokenExpirationDays    = 0.02083333333
check_auth_function_name = "prod-mls-user-service-media-check-auth"
domains                  = ["usermedia.myskillspring.cognizant.com"]
cf_distribution_name     = "prod-mls-user-service-media"


#Lambda Function Environment Variables
lambda_function_environment_variables = {
  APP_ENV                                       = "prod"  
  AWS_DYNAMODB_ENDPOINT                         = "https://dynamodb.ap-south-1.amazonaws.com"
  AWS_DYNAMODB_ROLES_TABLE_NAME                 = "prod-mls-user-roles-table"
  AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME   = "pk"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY      = "createdOn"
  AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER    = "desc"
  AWS_DYNAMODB_USER_TABLE_NAME                  = "prod-mls-user-service-table"
  AWS_DYNAMODB_USER_AUDIT_LOG_TABLE_NAME        = "prod-mls-user-audit-log-table"
  AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME    = "tenantCode"
  AWS_S3_BUCKET_NAME                            = "prod-mls-user-service-s3"
  AWS_ICONS_S3_BUCKET_NAME                      = "prod-mls-user-service-icons-s3"
  REGION_NAME                                   = "ap-south-1"
  THUMBNAIL_BUCKET_REGION_NAME                  = "us-east-1"
  DEFAULT_ROWS_PER_PAGE                         = 10
  LOCAL_STORAGE_PATH                            = "localPath"
  AWS_DYNAMODB_LOGFILE_TABLE_NAME               = "prod-mls-operations-history-table"
  AWS_DYNAMODB_LOGFILE_TABLE_PARTITION_KEY_NAME = "pk"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_KEY   = "createdOn"
  AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_ORDER = "desc"
  AWS_SQS_QUEUE_URL                             = "https://sqs.ap-south-1.amazonaws.com/274198321934/prod-mls-email-service-sqs"
  LMS_ADMIN_EMAIL                               = "skillspringsupport@cognizant.com"
  LMS_FROM_EMAIL_ADDRESS                        = "mylearning@skillspring.cognizant.com"
  LMS_URL                                       = "https://myskillspring.cognizant.com"
  AWS_COGNITO_CERT_URL                          = "https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_3Dwr8xyn5/.well-known/jwks.json"
  AWS_COGNITO_ISSUER                            = "https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_3Dwr8xyn5"  
  CORS_URL                                      = "https://myskillspring.cognizant.com,https://*.myskillspring.cognizant.com"
  COURSE_MANAGEMENT_SERVICE_API_URL             = "https://courseservice.myskillspring.cognizant.com"
  AWS_DYNAMODB_SKILL_LOOKUPS_TABLE_NAME         = "prod-mls-user-skills-table"
  ROLE_ARN                                      = "arn:aws:iam::274198321934:role/cross-account-cognito-role"
  AWS_DYNAMODB_TENANT_TABLE_NAME                = "prod-mls-tenant-table"
  AWS_DYNAMODB_TENANT_CONFIG_TABLE_NAME         = "arn:aws:dynamodb:ap-south-1:478340992552:table/prod-mls-tenant-config-table"
  AWS_COGNITO_DOMAIN                            = "https://idp.myskillspring.cognizant.com"
  AWS_LAMBDA_EXEC_WRAPPER                      = "/opt/datadog_wrapper"
  DD_API_KEY_SECRET_ARN                        = "arn:aws:secretsmanager:ap-south-1:274198321934:secret:Datadog-Keys-f062221d-hoJKfC"
  DD_SITE                                      = "datadoghq.com"  
  DD_TRACE_OTEL_ENABLED                        = false
  DD_PROFILING_ENABLED                         = false
  DD_SERVERLESS_APPSEC_ENABLED                 = false
  JAVA_TOOL_OPTIONS                            = "-javaagent:/var/task/dd-java-agent.jar"
  DD_TRACE_ENABLED                             = false
  AWS_DYNAMODB_LOOKUP_TABLE_NAME              = "prod-mls-user-lookup-table"
  DD_ENV                                       = "PROD"
  DD_INSTRUMENTATION_TELEMETRY_ENABLED           = false
  DD_FLUSH_TO_LOGS                               = false  
  DD_SERVICE                                     = "https://userservice.myskillspring.cognizant.com"
  DD_TAGS                                      = "appid:lms-user-service"
  AWS_DYNAMODB_USER_ACTIVITY_LOG_TABLE_NAME = "prod-mls-user-activity-log-table"
  AWS_USER_MANAGEMENT_EVENT_DETAIL_TYPE        = "UserManagement"
  AWS_USER_MANAGEMENT_EVENT_SOURCE             = "user.status"
  AWS_USER_MANAGEMENT_EVENT_BUS_ARN            = "arn:aws:events:ap-south-1:478340992552:event-bus/prod-mls-user-management-event-bus"
  AWS_DYNAMODB_USER_GLOBAL_SEARCH_HISTORY_TABLE_NAME = "prod-mls-user-global-search-history-table"
  ROOT_DOMAIN_PATH                                = "myskillspring.cognizant.com"
  DEFAULT_TENANT                                 = "cognizant"
  LOCALE_DATA_TABLE_NAME                         = "prod-mls-locale-data-table"
  AWS_S3_AI_VOICE_PREVIEW_BUCKET_NAME          = "prod-mls-user-service-s3"
  ALLOWED_DOMAINS = "myskillspring.cognizant.com"
  UI_PREFIXES = "ai,learner-catalog"
}

# DynamodDB Tables
dynamo_db_tables = {
  "prod-mls-user-service-table" : {
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
      "arn:aws:iam::274198321934:role/prod-mls-cognito-pretoken-lambda"
    ]
  },
  "prod-mls-tenant-table" : {
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
      "arn:aws:iam::274198321934:role/prod-mls-cognito-pretoken-lambda"
    ]
  },
  "prod-mls-user-roles-table" : {
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
  "prod-mls-operations-history-table" : {
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
  
  "prod-mls-user-skills-table" : {
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
  
  "prod-mls-user-lookup-table" : {
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
  "prod-mls-user-audit-log-table" : {
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
  "prod-mls-user-activity-log-table" : {
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
  "prod-mls-user-global-search-history-table" : {
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

  "prod-mls-locale-data-table" : {
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
environment    = "prod"
s3_bucket_name = "prod-mls-user-service-s3"
tenant_identifier = "myskillspring.cognizant.com"
add_item       = "true"

cognito_userpool_arn           = "arn:aws:cognito-idp:ap-south-1:274198321934:userpool/ap-south-1_3Dwr8xyn5"
cognito_cross_account_role_arn = "arn:aws:iam::274198321934:role/cross-account-cognito-role"
sqs_queue_arn                  = "arn:aws:sqs:ap-south-1:274198321934:prod-mls-email-service-sqs"
sqs_queue_failover_arn         = "arn:aws:sqs:us-east-1:274198321934:prod-mls-email-service-sqs-failover"
secret_manager_arn             = "arn:aws:secretsmanager:ap-south-1:274198321934:secret:Datadog-Keys-f062221d-hoJKfC"
kms_decrypt_arn                = "arn:aws:kms:ap-south-1:274198321934:key/f5f38b7b-fba8-478e-b904-ea5eea528358"

s3_icons_bucket_name           = "prod-mls-user-service-icons-s3"
#############################################
### Datadog Forwarder Variables ###
 
enable_datadog_forwarder          = true
enable_datadog_forwarder_failover = true
 
datadog_forwarder_arn          = "arn:aws:lambda:ap-south-1:478340992552:function:datadog-forwarder"
datadog_forwarder_arn_failover = "arn:aws:lambda:us-east-1:478340992552:function:datadog-forwarder"

 
log_retention_in_days = 30

## DynamoDB Trigger configurations ##
trigger_dynamodb_table = "prod-mls-user-service-table"

use_existing_vpc = true  
existing_vpc_id = "vpc-04bc2d7896738b008"
existing_private_subnets = ["subnet-0ee5cb04fce1da29e", "subnet-0fe0d9adebe4098b9"]
existing_security_group_ids = ["sg-089740091682108ad"]
use_existing_vpc_failover = true
existing_vpc_id_failover = "vpc-0e23c5da3cf65aa1a"
existing_private_subnets_failover = ["subnet-0868368d38982fba3", "subnet-0a9f340ff535aef20"]
existing_security_group_ids_failover = ["sg-0302487ed79ee853d"] 



### Event Bus Variables ###
event_bus_name            = "prod-mls-user-service-event-bus"
event_rule_name           = "user-activity-rule"
event_pattern_source      = "aws.cognito-idp"

user_management_event_bus_arn = "arn:aws:events:ap-south-1:478340992552:event-bus/prod-mls-user-management-event-bus"

CORS_URL         = ["https://myskillspring.cognizant.com", "https://*.myskillspring.cognizant.com"]

tenant_table                 = "prod-mls-tenant-config-table"
tenant_table_arn             = "arn:aws:dynamodb:ap-south-1:478340992552:table/prod-mls-tenant-config-table"
hostRootDomain               = "myskillspring.cognizant.com"

user-audit-log-function-name = "prod-mls-user-audit-log-lambda-function"
user-activity-log-table-name = "prod-mls-user-activity-log-table"
tenant_dynamodb_table = "prod-mls-tenant-table"
attach_response_headers_policy = true

policy_name_for_env = "CustomResponseHeadersPolicy_prod"

lms_credentials = "arn:aws:secretsmanager:ap-south-1:274198321934:secret:cognizant_prod_lms_credentials-JAqxvM"

