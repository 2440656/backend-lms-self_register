resource "aws_iam_role" "user_audit_log_lambda_role" {
  name = var.user-audit-log-lambda-role-name
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_policy" "user_audit_log_lambda_policy" {
  name        = var.user-audit-log-lambda-policy-name
  description = "Policy for Lambda to access DynamoDB and CloudWatch Logs"
  policy      = templatefile("${path.module}/lambda-policy-json.tftpl", {
    lambda_function_name = var.lambda-function-name
  })
}

resource "aws_iam_role_policy_attachment" "user_audit_log_lambda_policy_attach" {
  role       = aws_iam_role.user_audit_log_lambda_role.name
  policy_arn = aws_iam_policy.user_audit_log_lambda_policy.arn
}