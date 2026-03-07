package com.cognizant.lms.userservice.constants;

import java.util.List;

public class Constants {
  public static final int MAX_NAME_LENGTH = 100;
  public static final int MAX_EMAIL_LENGTH = 255;
  public static final int MAX_INSTITUTION_NAME_LENGTH = 255;
  public static final String USER_TYPE_INTERNAL = "Internal";
  public static final String USER_TYPE_EXTERNAL = "External";
  public static final List<String> USER_TYPES = List.of("Internal", "External");
  public static final int INITIAL_ROW_NUM = 1;
  public static final String EMAIL_PATTERN = "^[a-zA-Z0-9\\p{L}._%+-\\\\']+@[a-zA-Z0-9\\p{L}-]+(\\.[a-zA-Z]{2,})+$";
  public static final String FIELD_FIRST_NAME = "FirstName";
  public static final String FIELD_LAST_NAME = "LastName";
  public static final String FIELD_EMAIL_ID = "EmailID";
  public static final String FIELD_USER_TYPE = "UserType";
  public static final String FIELD_USER_ACCOUNT_EXPIRY_DATE = "AccountExpiryDate";
  public static final String FIELD_INSTITUTION_NAME = "InstitutionName";
  public static final String FIELD_ROLE = "Role";
  public static final List<String> FIELD_ROLES =
      List.of("learner", "super-admin", "mentor", "system-admin", "content-author",
          "catalog-admin");
  public static final String SUPER_ADMIN_ROLE = "super-admin";
    public static final String SYSTEM_ADMIN_ROLE = "system-admin";
  public static final String FILE_SAVED_MESSAGE = "File saved";
  public static final String storedInLocalMsg = "File saved to local storage";
  public static final String storedInS3Msg = "File saved to S3";
  public static final String appEnv = "local";
  public static final String fileFormat = ".csv";
  public static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
  public static final String S3_PREFIX = "/uploads/user-data/";
  public static final String ERROR_LOG_PREFIX = "/uploads/error-logs/";
  public static final String LOCAL_DISK_PREFIX_USERDATA = "\\user-data";
  public static final String LOCAL_DISK_PREFIX_ERRORLOG = "\\error-logs";
  public static final String ACTIVE_STATUS = "Active";
  public static final String MULTILINE_ERROR_LOG_FORMAT = """
      Uploaded File Name: %s
      Upload Time: %s
      Total Data Rows: %s
      Successful: %s
      Failed: %s
      Process: %s
            
      Information about failed records and the reasons for their failure.
      """;
  public static final String ROLE_LEARNER = "learner";
  public static final int COGNITO_TIMEOUT = 30;
  public static final int MAX_CONCURRENCY = 100;
  public static final int MAX_RETRIES = 3;
  public static final int RATE_LIMITER_TIMEOUT_SECS = 1;
  public static final int RATE_LIMITER_REFRESH_SECS = 1;
  public static final int RATE_LIMITER_PER_SEC = 50;
  public static final int MAX_BACKOFF_IN_MS = 5000;
  public static final String RATE_LIMITER_NAME = "cognitoRateLimiter";
  public static final String THROTTLING_EXCEPTION = "ThrottlingException";
  public static final String UNEXPECTED_ERROR = "UnexpectedError";
  public static final int DEFAULT_TEMP_PASSWORD_LENGTH = 16;
  public static final String UPPER_CASE_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String LOWER_CASE_STRING = "abcdefghijklmnopqrstuvwxyz";
  public static final String NUMBER_STRING = "0123456789";
  public static final String SPECIAL_CHARACTER_STRING = "~!@#$%^&*()_+.";
  public static final String LASTEVALUATED_KEY = "lastEvaluatedKey";
  public static final String USERS = "users";
  public static final String TOTAL_COUNT = "count";
  public static final String TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX";
  public static final String ERROR_MESSAGE = "No users found";
  public static final String FILE_TYPE_CSV = "csv";
  public static final String FILE_TYPE_TXT = "txt";
  public static final String LOG_FILE_SUFFIX = "_LOG_";
  public static final String LOG_FILE_EXTENSION = ".txt";
  public static final String LOG_FILE_TIMESTAMP_FORMAT = "MMddyyHHmm";
  public static final String TIMESTAMP_DATE_FORMAT = "MM-dd-yy HH:mm:ss";
  public static final String USER_EXPIRY_DATE_FORMAT = "MM/dd/yyyy";
  public static final String COMA = ",";
  public static final String HASH = "#";
  public static final String ATTACHMENT_FILENAME = "attachment; filename=";
  public static final String SLASH = "\\";
  public static final List<String> VALID_DEACTIVATE_USERS_HEADERS = List.of("EmailID");
  public static final List<String> deactivateUsersLogHeaders =
      List.of("TimeStamp", "EmailID", "Reason for Error");
  public static final List<String>  REACTIVEUSER_LOG_HEADER = List.of("TimeStamp", "EmailID","Expiry Date", "Reason for Error");
  public static final String ACTION_ADD = "Add Users";
  public static final String ACTION_UPDATE = "Update Users";
  public static final String ACTION_DELETE = "Delete Users";
  public static final String ACTION_REACTIVATE = "Reactivate users";
  public static final String ACTION_DEACTIVATE = "De-Activate Users";
  public static final List<String> uploadUsersLogHeaders =
      List.of("TimeStamp", "EmailID", "FirstName", "LastName", "Reason for Error");
  public static final List<String> VALID_ADD_USERS_HEADERS =
      List.of("FirstName", "LastName", "EmailID", "UserType", "AccountExpiryDate",
          "InstitutionName", "Role", "ViewOnlyAssignedCourses", "LoginOption", "Country");
  public static final List<String> VALID_REACTIVATE_USER = List.of("EmailID","ExpiryDate");
  public static final String USER_EXPIRY_DATE = "ExpiryDate";
  public static final String USER_ACCOUNT_EXPIRYDATE = "userAccountExpiryDate";
  public static final String IN_ACTIVE_STATUS = "Inactive";
  public static final String DEFAULT_GSI = "gsi_sort_createdOn";
  public static final String FIRST_AS_YEAR_FORMAT = "yyyy/MM/dd";
  public static final String FIRST_AS_MONTH_FORMAT = "MM/dd/yyyy";
  public static final String CREATED_ON_UPDATED_ON_TIMESTAMP = "yyyy/MM/dd HH:mm:ss";
  public static final List<String> VALID_UPDATE_USERS_HEADERS =
      List.of("FirstName", "LastName", "EmailID", "AccountExpiryDate",
          "InstitutionName", "Country");
  public static final String PARTITION_KEY_NAME = "pk";
  public static final String ALPHABET_REGEX = "[a-zA-Z' ]+";
  public static final String COMMA_REGEX = "\\s*,\\s*";
  public static final String AREA_USER_MANAGEMENT = "user-man";
  public static final String SUBJECT_VALUE = "Welcome to Cognizant Skillspring – Your Learning Journey Begins!";
  public static final String SKILLSPRING_USER_WELCOME_NOTIFICATION = "SkillSpring User Welcome Email";
  public static final String SOCIAL_IDP_USER_WELCOME_NOTIFICATION = "SocialIDP User Welcome Email";
    public static final String MIGRATION_SKILL = "Migration Skill";
  public static  final String SUBJECT_REMINDER_EMAIL_FOR_INITIAL_DEACTIVATION= "Action Required: Access your Cognizant Skillspring account within {{No_Of_Days}} days to keep it active!";
  public static final String SUBJECT_REMINDER_EMAIL_FOR_DAILY_DEACTIVATION_="Action Required: Access your Cognizant Skillspring account within {{No_Of_Days}} days!";
  public static final String IMAGE_WIDTH = "100%";
  public static final String TO = "to";
  public static final String FROM = "from";
  public static final String CC= "cc";
  public static final String SUBJECT = "subject";
  public static final String BODY = "body";
  public static final String EMAIL_DETAILS = "emailDetails";
  public static final String CLIENT_DETAILS = "clientDetails";
  public static final String CLIENT_ID = "clientId";
  public static final String CLIENT_ID_VALUE = "user-service";
  public static final String NOTIFICATION_EVENT_NAME =  "notificationEventName";
  public static final String IMAGE_PATH_LOCAL = "src/main/resources/static/images/emailHeader.png";
  public static final String TEMPLATE_PATH_LOCAL =
      "src/main/resources/static/templates/email-template.txt";
  public static final String CUSTOM_LOGGER_REQUEST_ID = "requestId";
  public static final String CUSTOM_LOGGER_TENANT_CODE = "tenantCode";
  public static final String CUSTOM_LOGGER_SERVICE_NAME = "serviceName";
  public static final String CUSTOM_LOGGER_PROCESS_NAME = "processName";
  public static final String CUSTOM_LOGGER_PROCESS_STATUS = "processStatus";
  public static final String CUSTOM_LOGGER_PROCESS_MESSAGE = "processMessage";
  public static final String CUSTOM_LOGGER_ERROR_CODE = "errorCode";
  public static final String CUSTOM_LOGGER_ERROR_MESSAGE = "errorMessage";
  public static final String CUSTOM_LOGGER_STACKTRACE = "stacktrace";
  public static final String IMAGE_NAME = "emailHeader.png";
  public static final String WELCOME_EMAIL_TEMPLATE_NAME = "WelcomeEmailTemplate.txt";
  public static final String WELCOME_EMAIL_TEMPLATE_NAME_SOCIAL_IDP = "WelcomeEmailTemplateSocialIDP.txt";
  public static final String ROLE_ASSIGNMENT_EMAIL_TEMPLATE="LearnerAdditionalRoleAddedTemplate.txt";
  public static final String TEMPLATE_PATH = "/templates/";
  public static final String FILE_TYPE_TEMPLATE = "template";
  public static final String FILE_TYPE_IMAGE = "image";
  public static final String LOG_FILES = "logFiles";
  public static final String IMAGE_PATH = "/images/";
  public static final String COOKIE_NAME_PREFIX = "CognitoIdentityServiceProvider.";
  public static final String ID_TOKEN_COOKIE_NAME_SUFFIX = ".idToken";
  public static final String SYSTEM = "System";
  public static final String NO_EXPIRED_USER_FOUND = "No expired users found";
  public static final String EXPIRED_USER_DEACTIVATED = "Expired user deactivated";
  public static final String TENANT_CODE = "t-2";
  public static final String TRACKER_ID = "trackerId";
  public static final String SUPER_ADMIN_TENANT_CODE = "t-1";
  public static final String USER_MANAGEMENT = "#user-man";
  public static final String IGNORE_DATE = "1900/01/01";
  public static final String USER_DEFAULT_EXPIRY_DATE= "2050/01/01";
  public static final String ALL_SKILL = "doma,func,tech,beha";
  public static final String AWS_CROSS_ACCOUNT_ROLEARN =
      "arn:aws:iam::039612885605:role/lms-cross-account-cognito-role";
  public static final String SbxEnv = "sbx";
  public static final String YES = "Y";
  public static final String NO = "N";
  public static final String FIELD_VIEWONLY_ASSIGNED_COURSES = "ViewOnlyAssignedCourses";
  public static final String ROLE_CONTENT_AUTHOR = "content-author";
  public static final String FIELD_LOGIN_OPTION = "LoginOption";
  public static final String LOGIN_OPTION_LMS_CREDENTIALS = "Skillspring Credentials";
  public static final String LOGIN_OPTION_SOCIAL_IDP = "Social IDP";
  public static final String SOCIAL_IDP_GITHUB = "Github";
  public static final String SOCIAL_IDP_GOOGLE = "Google";
  public static final String LOGIN_OPTION_COGNIZANT_SSO = "CognizantSSO";
  public static final String TENANT_HEADER = "x-tenant-id";
  public static final String IMAGES_PATH_PREFIX_S3 = "thumbnailImages/";
  public static final String DOCUMENT_PATH_PREFIX_S3 = "courseDocuments/";
  public static final String S3_CONTENT_PREFIX = "/content/";
  public static final String DOT = ".";
  public static final String EMPTY_STRING = "";
  public static final String ACCESS_TOKEN_COOKIE_NAME_SUFFIX = ".accessToken";
  public static final String REFRESH_TOKEN_COOKIE_NAME_SUFFIX = ".refreshToken";
  public static final String NEW_ROLE_ASSIGNMENT_TO_USER = "New Role Assigned on Cognizant Skillspring";
  public static final String NEW_ROLE_ASSIGNMENT_NOTIFICATION = "Role Assigned";
  public static final List<String> TRIGGER_NOTIFICATION_FOR_ADDITIONAL_ROLES = List.of("content-author", "system-admin", "roster-management-admin");
  public static final String TENANT_SETTING_NAME = "content-moderation";
  public static final String SETTING = "settings";
  public static final String TENANT_SETTING_GSI_TYPE = "gsi_type";
  public static final String HELP_RESOURCE_URL="/#/main/help-resources";
  public static final String TEMP_PASSWORD_REMINDER_SUBJECT = "Action Required: Your LMS Password Has Expired-Temporary Password Issued";
  public static final String TEMP_PASSWORD_NOTIFICATION = "Temporary Password Issued";
  public static final String TEMP_PASSWORD_TEMPLATE_NAME = "TemporaryPasswordEmailTemplate.txt";
  public static final String NO_USER = "NO_USER_IN_DB";
  public static final String FIELD_COUNTRY = "Country";
  public static final String DEFAULT_COUNTRY = "-";
  public static final String LOOKUP_INDEX_NAME = "gsi_sort_name";
  public static final String REGISTER_LOGIN_API = "api/v1/users/current-user/register-login";
  public static final String REACTIVATE_USER_SUBJECT = "Attention: Your Cognizant Skillspring account has been reactivated";public static final String REACTIVATE_USER_NOTIFICATION = "Reactivate User";
  public static final String REACTIVATE_SKILLSPRING_USER_SUBJECT = "Your Cognizant Skillspring account has been reactivated – Action required";
  public static final String REACTIVATE_SKILLSPRING_USER_NOTIFICATION = "Reactivate SkillSpring User";
  public static final String REACTIVATE_SKILLSPRING_USER_EMAIL_TEMPLATE_NAME = "ReactivateSkillSpringUserEmailTemplate.txt";
  public static final String REACTIVATE_USER_EMAIL_TEMPLATE_NAME = "ReactivateUserEmailTemplate.txt";
  public static final String INTEGRATION_FIELD_TYPE = "IntegrationField";
  public static final String SETTINGS_IDENTIFIERS_TYPE = "UniqueIdentifier";
  public static final String CONTENT_TYPE_MAPPING_TYPE = "ContentTypeMapping";
  public static final String CATEGORY_TYPE_MAPPING_TYPE = "CategoryTypeMapping";
  public static final String COMPLETION_SYNC_MAPPING_TYPE = "CompletionSyncMapping";
  public static final String METADATA_MAPPING_TYPE = "MetaData";
  public static final String LESSON_METADATA_MAPPING_TYPE = "LessonMetaData";

  public static final String INTEGRATION_FIELD_PREFIX = "IntegrationField#";
  public static final String METADATA_MAPPING_PREFIX = "MetadataMapping#";
  public static final String LESSON_METADATA_MAPPING_PREFIX="LessonMetadataMapping#";

  public static final String SETTINGS_IDENTIFIERS_PREFIX = "UniqueIdentifier#";
  public static final String CONTENT_TYPE_MAPPING_PREFIX = "ContentTypeMapping#";
  public static final String CATEGORY_TYPE_MAPPING_PREFIX = "CategoryTypeMapping#";
  public static final String COMPLETION_SYNC_MAPPING_PREFIX = "CompletionSyncMapping#";
  public static final String GENERAL_INFORMATION_PAGE = "GeneralInformation";
  public static final String CORE_CONFIGURATION_PAGE = "CoreConfiguration";
  public static final String SETTINGS_PAGE = "Settings";
  public static final String CONTENT_MAPPING_PAGE = "ContentMapping";
  public static final String METADATA_MAPPING_PAGE = "MetadataMapping";
  public static final String LESSON_MAPPING_PAGE = "LessonMetadataMapping";

  public static final String INTEGRATION_TYPE = "Integration";
  public static final String DRAFT_STATUS = "Draft";
  public static final String CATEGORY_MAPPING = "CategoryMapping";
  public static final String SFTP_CATEGORY_TYPE_MAPPING_PREFIX = "SftpCategoryTypeMapping#";
  public static final String SFTP_CATEGORY_TYPE_MAPPING_TYPE = "SftpCategoryTypeMapping";


  
  //Skills Master Data Constants
  public static final String ACTION_UPLOAD_SKILLS = "Upload Skills";
  public static final String S3_SKILLS_MASTERDATA_PREFIX = "/upload/skills-masterData/";
  public static final String SKILLS_ERROR_LOG_PREFIX = "/upload/skills-error-logs/";
  public static final String LOCAL_DISK_PREFIX_SKILLS_MASTERDATA = "\\skills-masterData";
  public static final String LOCAL_DISK_PREFIX_SKILLS_ERRORLOG = "\\skills-error-logs";
  public static final String AREA_SKILL_MANAGEMENT = "skills-mgmt";
  public static final  String SKILL_MANAGEMENT = "#skills-mgmt";
  public static final List<String> VALID_SKILL_MASTER_HEADERS = List.of(
          "skillCode", "skillName", "skillDescription", "skillType", "status", "skillCategory", "skillSubCategory"
  );
  public static final List<String> uploadSkillsLogHeaders = List.of(
          "TimeStamp", "SkillCode", "SkillName", "SkillType", "Reason for Error"
  );

  public static final String SKILL_NAME_REGEX = "^[\\p{L}\\p{N}\\p{P}\\p{S} ]+$";
  public static final String SKILL_LOG_CREATED_ON_TIMESTAMP = "yyyy/MM/dd HH:mm:ss";
  public static final String INPROGRESS = "inProgress";
  public static final String Completed = "Completed";
  public static final String ADD_USER_EVENT = "addUserEvent";
  public static final String ADD_BULK_USER_ACTION = "AddBulkUsers";
  public static final String FAILED = "Failed";
  public static final int MAX_ROWS_IN_CSV_FOR_BULK_USERS = 1000;
  public static final String ZERO_PERCENT = "0%";
  public static final String HUNDRED_PERCENT = "100%";
  public static final String STATUS_IN_PROGRESS = "In-Progress";
  public static final int MAX_POPULAR_LINKS = 20;
  public static final int MAX_QUICK_LINKS = 20;
  public static final String SKILL_PROF_LEVEL = "Skill-Prof-Level";
  public static final String AUTHORIZATION = "Authorization";
  public static final String CONFIGURATION = "configuration";
  public static final String MLS_ENROLL_ENDPOINT = "api/v1/lmsusers";
  public static final String TENANTTYPE = "tenant";
  public static final String KEYWORD = "KW";
  public static final String USER_KEYWORD = "UserKeyword";
  public static final String USER_PK = "USER";
  public static final String ACTIVE = "ACTIVE";
  public static final String TIME_STAMP = "TS";
  public static final String USER_ACTIVITY_LOGIN_TYPE = "LOGIN";
  public static final String USER_ACTIVITY_LOGOUT_TYPE = "LOGOUT";
  public static final String USER_ACTIVITY_SUCCESS_STATUS = "success";
  public static final String GSI_USER_SEARCH_HISTORY = "gsi_userSearchHistory";
  public static final String COGNIZANT_TENANT_CODE = "cognizant";
  public static final String SFTP_INTEGRATION_TYPE = "SFTPIntegration";
  public static final String UPDATED_DATE = "updatedDate";
  public static final String ACTIVE_INTEGRATION = "Active";
  public static final String QUICKLINK_PREFIX = "QUICKLINK#";
  public static final String SERVICELINE_PREFIX = "SERVICELINE#";
  public static final String USER_SETTINGS = "userSettings";
  public static final String PREFERRED_UI_CLASSIC = "classic";
  public static final String TYPE_USER_SETTINGS = "userSettings";
  public static final String GSI_EMAIL_ID_SK = "gsi_emailId_sk";
  public static final String LOCAL = "local";
  public static final String SKILL = "Skill";
  public static final String GSI_SKILL_NAME = "gsi_skillName";
  public static final String GSI_SKILL_CODE = "gsi_skillCode";
}
