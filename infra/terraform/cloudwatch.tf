resource "aws_cloudwatch_log_group" "gateway" {
  name              = "/ecs/${var.prefix}-gateway"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "chatbot" {
  name              = "/ecs/${var.prefix}-chatbot"
  retention_in_days = 14
}
