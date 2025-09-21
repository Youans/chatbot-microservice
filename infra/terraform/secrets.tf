resource "aws_secretsmanager_secret" "openai" {
  count = (var.openai_secret_name == null || var.openai_api_key == null) ? 0 : 1
  name  = var.openai_secret_name
}

resource "aws_secretsmanager_secret_version" "openai" {
  count         = (var.openai_secret_name == null || var.openai_api_key == null) ? 0 : 1
  secret_id     = aws_secretsmanager_secret.openai[0].id
  secret_string = jsonencode({ OPENAI_API_KEY = var.openai_api_key })
}
