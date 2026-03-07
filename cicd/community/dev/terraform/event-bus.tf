resource "aws_cloudwatch_event_bus" "custom_event_bus" {
  name = var.event_bus_name
}

resource "aws_cloudwatch_event_rule" "cognito-login-logout-activities" {
  name           = var.event_rule_name
  description    = "Rule for learner.status and LessonUpdate"
  event_bus_name = aws_cloudwatch_event_bus.custom_event_bus.name
  event_pattern = jsonencode({
    "source" : [var.event_pattern_source]
  })
}

### Cognito Permissions events to EventBridge ###
resource "aws_cloudwatch_event_permission" "publish_event_cognito" {
  principal      = var.aws_shared_services_account_id
  statement_id   = "PublishEventCognito"
  action         = "events:PutEvents"
  event_bus_name = aws_cloudwatch_event_bus.custom_event_bus.name
}


# IAM role for EventBridge to invoke the Lambda
resource "aws_iam_role" "eventbridge_invoke_lambda" {
  name = "${var.event_bus_name}-eventbridge-invoke-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "events.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })
}

# Attach policy to allow invoking the Lambda function
resource "aws_iam_role_policy" "allow_lambda_invoke" {
  name = "AllowInvokeLambda"
  role = aws_iam_role.eventbridge_invoke_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "lambda:InvokeFunction"
      Resource = "${module.lms_backend.lambda_function_arn}:*"
    }]
  })
}

# Attach Lambda function as target
resource "aws_cloudwatch_event_target" "lambda_target" {
  rule           = aws_cloudwatch_event_rule.cognito-login-logout-activities.name
  target_id      = "SendToLambda"
  arn            = "${module.lms_backend.lambda_function_arn}:${var.lambda_alias_name}"
  role_arn       = aws_iam_role.eventbridge_invoke_lambda.arn
  event_bus_name = aws_cloudwatch_event_bus.custom_event_bus.name
}
