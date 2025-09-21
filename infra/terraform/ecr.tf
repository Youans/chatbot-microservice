resource "aws_ecr_repository" "gateway" {
  name                 = "${var.prefix}-gateway"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

resource "aws_ecr_repository" "chatbot" {
  name                 = "${var.prefix}-chatbot"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
}
