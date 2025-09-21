resource "aws_dynamodb_table" "chat_messages" {
  name         = "${var.prefix}-chat_messages"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "sessionId"
  range_key    = "ts"

  attribute {
    name = "sessionId"
    type = "S"
  }

  attribute {
    name = "ts"
    type = "N"
  }

  tags = {
    Name = "${var.prefix}-chat_messages"
  }
}
