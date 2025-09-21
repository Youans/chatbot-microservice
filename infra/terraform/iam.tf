data "aws_iam_policy_document" "ecs_task_execution_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_task_execution" {
  name               = "${var.prefix}-ecs-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_execution_assume.json
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Task role for app containers
resource "aws_iam_role" "app_task_role" {
  name               = "${var.prefix}-app-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_execution_assume.json
}

# Allow reading secrets (OpenAI key), DynamoDB, and CloudWatch/X-Ray if needed
resource "aws_iam_role_policy" "app_task_inline" {
  name = "${var.prefix}-app-task-inline"
  role = aws_iam_role.app_task_role.id

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action   = ["secretsmanager:GetSecretValue"],
        Resource = var.openai_secret_name == null ? "*" : "arn:aws:secretsmanager:${var.region}:*:secret:${var.openai_secret_name}*"
      },
      {
        Effect = "Allow",
        Action = [
          "dynamodb:PutItem",
          "dynamodb:Query",
          "dynamodb:GetItem",
          "dynamodb:BatchWriteItem"
        ],
        Resource = "arn:aws:dynamodb:${var.region}:*:table/${var.prefix}-chat_messages"
      },
      {
        Effect   = "Allow",
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents"],
        Resource = "*"
      }
    ]
  })
}
