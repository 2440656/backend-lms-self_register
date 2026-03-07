locals {
  lambda_alias_arn          = "${module.lms_backend.lambda_function_arn}:${var.lambda_alias_name}"
}

data "aws_dynamodb_table" "trigger_table" {
  name     = var.trigger_dynamodb_table
}

data "aws_dynamodb_table" "user_activity_log_trigger_table" {
  name = var.user-activity-log-table-name
}

data "aws_dynamodb_table" "trigger_tenant_table" {
  name     = var.tenant_dynamodb_table
}

resource "aws_lambda_event_source_mapping" "dynamodb_trigger" {
  count             = data.aws_dynamodb_table.trigger_table.stream_arn != null ? 1 : 0
  event_source_arn  = data.aws_dynamodb_table.trigger_table.stream_arn
  function_name     = module.lms_backend.lambda_function_arn
  starting_position = "LATEST"

  tags = merge(
    local.common_tags,
    {
      Environment = var.environment
    }
  )

  depends_on = [module.lms_backend]
}

resource "aws_lambda_event_source_mapping" "dynamodb_trigger_user_audit_log" {
  count             = data.aws_dynamodb_table.trigger_table.stream_arn != null ? 1 : 0
  event_source_arn  = data.aws_dynamodb_table.trigger_table.stream_arn
  function_name     = var.user-audit-log-function-name
  enabled = true
  starting_position = "LATEST"

  tags = merge(
    local.common_tags,
    {
      Environment = var.environment
    }
  )
}

resource "aws_lambda_event_source_mapping" "dynamodb_trigger_user_activity_log" {
  count             = data.aws_dynamodb_table.user_activity_log_trigger_table.stream_arn != null ? 1 : 0
  event_source_arn  = data.aws_dynamodb_table.user_activity_log_trigger_table.stream_arn
  function_name     = var.user-audit-log-function-name
  enabled = true
  starting_position = "LATEST"

  tags = merge(
    local.common_tags,
    {
      Environment = var.environment
    }
  )
}

resource "aws_lambda_event_source_mapping" "dynamodb_trigger_tenant_table" {
  count             = data.aws_dynamodb_table.trigger_tenant_table.stream_arn != null ? 1 : 0
  event_source_arn  = data.aws_dynamodb_table.trigger_tenant_table.stream_arn
  function_name     = var.user-audit-log-function-name
  starting_position = "LATEST"

  tags = merge(
    local.common_tags,
    {
      Environment = var.environment
    }
  )
}
