# Adding Items to User Service Table

resource "aws_dynamodb_table_item" "user_service_table_item1" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_service_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
      "pk": {"S": "e92e1095-d380-4cf1-8210-96f7db9853g0"},
      "sk": {"S": "acad4b4b-1db7-4a9d-87f9-39912f96b17e"},
      "tenantCode": {"S": "cognizant"},
      "createdOn": {"S": "9/22/2025  3:55:32 AM"},
      "gsiSortFNLN": {"S": "Rajesh#Somarajan"},
      "status": {"S": "Active"},
      "userType": {"S": "Internal"},
      "institutionName": {"S": "Cognizant"},
      "firstName": {"S": "Rajesh"},
      "lastName": {"S": "Somarajan"},
      "role": {"S": "system-admin,content-author,learner,super-admin"},
      "emailId": {"S": "Rajesh.Somarajan@cognizant.com"},
      "name": {"S": "rajeshsomarajan"},
      "userAccountExpiryDate": {"S": "12/21/2035"},
      "lastLoginTimestamp": {"S": "2025-09-22T09:56:23.900567732Z"},
      "modifiedBy": {"S": "system"},
      "modifiedOn": {"S": "9/25/2025  0:00:00 AM"}
  }
  ITEM
}

# Adding Items to User Roles Table

resource "aws_dynamodb_table_item" "user_role_table_item1" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {    
    "pk": {"S": "Role"},
    "sk": {"S": "catalog-admin"},
    "active": {"S": "TRUE"},
    "description": {"S": "Catalog Admin"},
    "name": {"S": "Catalog Admin"},
    "type": {"S": "role"}
  }
  ITEM
}

resource "aws_dynamodb_table_item" "user_role_table_item2" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  { 
    "pk": {"S": "Role"},
    "sk": {"S": "super-admin"},
    "active": {"S": "TRUE"},
    "description": {"S": "Application Administrator"},
    "name": {"S": "Super Admin"},
    "type": {"S": "role"}
  }
  ITEM
}    

resource "aws_dynamodb_table_item" "user_role_table_item3" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {   
    "pk": {"S": "Role"},
    "sk": {"S": "system-admin"},
    "active": {"S": "TRUE"},
    "description": {"S": "Lenant Administrator"},
    "name": {"S": "System Admin"},
    "type": {"S": "role"}
  }
  ITEM    
}

  resource "aws_dynamodb_table_item" "user_role_table_item4" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  { 
    "pk": {"S": "Role"},
    "sk": {"S": "learner"},
    "active": {"S": "TRUE"},
    "description": {"S": "Learner"},
    "name": {"S": "Learner"},
    "type": {"S": "role"}
  }
  ITEM    
}

resource "aws_dynamodb_table_item" "user_role_table_item5" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
    "pk": {"S": "Role"},
    "sk": {"S": "mentor"},
    "active": {"S": "TRUE"},
    "description": {"S": "Mentor"},
    "name": {"S": "Mentor"},
    "type": {"S": "role"}
  }  
  ITEM    
}

resource "aws_dynamodb_table_item" "user_role_table_item6" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
    "pk": {"S": "Role"},
    "sk": {"S": "content-author"},
    "active": {"S": "TRUE"},
    "description": {"S": "Content Author"},
    "name": {"S": "Content Author"},
    "type": {"S": "role"}
  }  
  ITEM    
}

resource "aws_dynamodb_table_item" "user_role_table_item7" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
    "pk": {"S": "Role"},
    "sk": {"S": "async-facilitator"},
    "active": {"S": "TRUE"},
    "description": {"S": "Async Facilitator"},
    "name": {"S": "Async Facilitator"},
    "type": {"S": "role"}
  }
  ITEM
}

resource "aws_dynamodb_table_item" "user_role_table_item8" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
    "pk": {"S": "Role"},
    "sk": {"S": "learner-delivery-admin"},
    "active": {"S": "TRUE"},
    "description": {"S": "Learner Delivery Admin"},
    "name": {"S": "Learner Delivery Admin"},
    "type": {"S": "role"}
  }
  ITEM
}

resource "aws_dynamodb_table_item" "user_role_table_item9" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.user_roles_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
    "pk": {"S": "Role"},
    "sk": {"S": "roster-management-admin"},
    "active": {"S": "TRUE"},
    "description": {"S": "Roster Management Admin"},
    "name": {"S": "Roster Management Admin"},
    "type": {"S": "role"}
  }
  ITEM
}

# Adding Items to Tenant Table

resource "aws_dynamodb_table_item" "tenant_table_item1" {
  count      = var.add_item ? 1 : 0
  depends_on = [module.lms_backend]
  table_name = var.tenant_table_name
  hash_key   = "pk"
  range_key  = "sk"

  item = <<ITEM
  {
    "pk": {"S": "cognizant"},
    "sk": {"S": "cognizant#2025/10/04"},
    "idpPreferences": {"S": "CognizantSSO,Skillspring Credentials"},
    "createdOn": {"S": "2025/09/22"},
    "type": {"S": "tenant"},
    "tenantIdentifier": {"S": "${var.tenant_identifier}"},
    "userPoolId": {"S": "${local.secret_values.AWS_COGNITO_USER_POOL_ID}"},
    "name": {"S": "Cognizant"},
    "status": {"S": "Active"},
    "configurations": {"S": "{\"deactivateUserThreshold\":180,\"deactivateUserInitialNotificationInDays\":165,\"deactivateUserReminderNotificationInDays\":175}"}
  }
  ITEM
}