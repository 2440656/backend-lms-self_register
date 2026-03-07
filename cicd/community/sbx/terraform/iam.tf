resource "aws_iam_policy" "lambda_s3_access_policy" {
  name        = "${var.api_lambda_function_name}-s3"
  description = "Policy to allow Lambda read and write access to S3 bucket"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket",
          "s3:DeleteObject"
        ]
        Resource = [
          "${module.s3_bucket.bucket_arn}",
          "${module.s3_bucket.bucket_arn}/*",
          "arn:aws:s3:::${var.s3_icons_bucket_name}",
          "arn:aws:s3:::${var.s3_icons_bucket_name}/*"
          ]
      },
      {
        Effect = "Allow"
        Action = [
          "events:PutEvents"
        ]
        Resource = [
          "${var.user_management_event_bus_arn}"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_s3_policy_attachment" {
  role       = module.lms_backend.lambda_role_name
  policy_arn = aws_iam_policy.lambda_s3_access_policy.arn
}

// Attach additional policy to Failover Lambda role
resource "aws_iam_role_policy_attachment" "lambda_s3_policy_attachment_failover" {
  count     = local.configure_failover ? 1 : 0
  role       = module.lms_backend.lambda_failover_role_name
  policy_arn = aws_iam_policy.lambda_s3_access_policy.arn
}


resource "aws_iam_policy" "cognito_access_policy" {
  name        = "${var.api_lambda_function_name}-cognito"
  description = "Policy for permissions on Cognito UserPool in Shared Account"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AssumeRole"
        Effect = "Allow"
        Action = [
          "sts:AssumeRole"
        ]
        Resource = "${var.cognito_cross_account_role_arn}"
      },
      {
        Sid    = "Cognito"
        Effect = "Allow"
        Action = [
          "cognito-idp:AdminCreateUser",
          "cognito-idp:AdminUpdateUserAttributes",
          "cognito-idp:AdminDeleteUser",
          "cognito-idp:AdminGetUser",
          "cognito-idp:AdminSetUserPassword"
        ]
        Resource = "${var.cognito_userpool_arn}"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "cognito_access_policy_attachment" {
  role       = module.lms_backend.lambda_role_name
  policy_arn = aws_iam_policy.cognito_access_policy.arn
}

resource "aws_iam_role_policy_attachment" "cognito_access_policy_attachment_failover" {
  count     = local.configure_failover ? 1 : 0
  role       = module.lms_backend.lambda_failover_role_name
  policy_arn = aws_iam_policy.cognito_access_policy.arn
}

resource "aws_iam_policy" "sqs_policy" {
  name        = "${var.api_lambda_function_name}-sqs-policy"
  description = "Policy for permissions on SQS in Shared Account"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Sqs"
        Effect = "Allow"
        Action = [
          "sqs:sendmessage"
        ]
        Resource = "${var.sqs_queue_arn}"
      }
    ]
  })
}

# resource "aws_iam_policy" "sqs_policy_failover" {
#   name        = "${var.api_lambda_function_name}-sqs-policy-failover"
#   description = "Policy for permissions on SQS in Shared Account"
#   policy = jsonencode({
#     Version = "2012-10-17"
#     Statement = [
#       {
#         Sid    = "Sqs"
#         Effect = "Allow"
#         Action = [
#           "sqs:sendmessage"
#         ]
#         Resource = "${var.sqs_queue_failover_arn}"
#       }
#     ]
#   })
# }

resource "aws_iam_role_policy_attachment" "sqs_policy_attachment" {
  role       = module.lms_backend.lambda_role_name
  policy_arn = aws_iam_policy.sqs_policy.arn
}

# resource "aws_iam_role_policy_attachment" "sqs_policy_attachment_failover" {
#   count     = local.configure_failover ? 1 : 0
#   role       = module.lms_backend.lambda_failover_role_name
#   policy_arn = aws_iam_policy.sqs_policy_failover.arn
# }
/*
resource "aws_iam_policy" "secrets_policy" {
  name        = "${var.api_lambda_function_name}-secrets-policy"
  description = "Policy for permissions on Secrets in Shared Account"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Secretmanager"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = "${var.secret_manager_arn}"
      },
      {
        Sid    = "KMSDecrypt"
        Effect = "Allow"
        Action = [
          "kms:Decrypt"
        ]
        Resource = "${var.kms_decrypt_arn}"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "secrets_policy_attachment" {
  role       = module.lms_backend.lambda_role_name
  policy_arn = aws_iam_policy.secrets_policy.arn
}

resource "aws_iam_role_policy_attachment" "secrets_policy_attachment_failover" {
  count     = local.configure_failover ? 1 : 0
  role       = module.lms_backend.lambda_failover_role_name
  policy_arn = aws_iam_policy.secrets_policy.arn
}
*/
# Permissions for DynamoDB Trigger

resource "aws_iam_policy" "dynamodb_trigger_policy" {
  count       = data.aws_dynamodb_table.trigger_table.stream_arn != null ? 1 : 0
  name        = "${var.api_lambda_function_name}-dynamodb-trigger"
  description = "Policy to add DynamoDB Trigger"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "dynamodb:GetRecords",
          "dynamodb:GetShardIterator",
          "dynamodb:DescribeStream",
          "dynamodb:ListStreams"
        ],
        Resource = [
          data.aws_dynamodb_table.trigger_table.stream_arn
          # data.aws_dynamodb_table.trigger_tenant_table.stream_arn
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_dynamodb_trigger_attachment" {
  count      = data.aws_dynamodb_table.trigger_table.stream_arn != null ? 1 : 0
  role       = module.lms_backend.lambda_role_name
  policy_arn = aws_iam_policy.dynamodb_trigger_policy[0].arn
}

resource "aws_iam_role_policy_attachment" "lambda_dynamodb_trigger_attachment_failover" {
  count      = local.configure_failover && data.aws_dynamodb_table.trigger_table.stream_arn != null ? 1 : 0
  role       = module.lms_backend.lambda_failover_role_name
  policy_arn = aws_iam_policy.dynamodb_trigger_policy[0].arn
}

resource "aws_iam_policy" "user_service_dynamodb_lambda_policy" {
  name        = "${var.api_lambda_function_name}-dynamodb"
  description = "Policy for DynamoDB Permissions on Lambda"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        "Sid" : "DynamoDB",
        "Effect" : "Allow",
        "Action" : [
          "dynamodb:GetItem",
          "dynamodb:BatchGetItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:ConditionCheckItem",
          "dynamodb:DescribeTable",
          "dynamodb:ListTables"
        ],
        "Resource" : [
          "arn:aws:dynamodb:${var.aws_region}:${var.tenant_management_account}:table/${var.tenant_table}",
          "arn:aws:dynamodb:${var.aws_region}:${var.tenant_management_account}:table/${var.tenant_table}/index/*",
          "arn:aws:dynamodb:${var.aws_failover_region}:${var.tenant_management_account}:table/${var.tenant_table}",
          "arn:aws:dynamodb:${var.aws_failover_region}:${var.tenant_management_account}:table/${var.tenant_table}/index/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "course_dynamodb_policy_attachment" {
  depends_on = [module.lms_backend]
  role       = module.lms_backend.lambda_role_name
  policy_arn = aws_iam_policy.user_service_dynamodb_lambda_policy.arn
}

### Pre-signed URL Upload
resource "aws_s3_bucket_cors_configuration" "cors_policy" {
  bucket = module.s3_bucket.bucket_name // changed here
  cors_rule {
    allowed_methods = ["PUT"]
    allowed_origins = var.CORS_URL
    allowed_headers = ["Content-Type"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
} 
