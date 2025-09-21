data "aws_iam_policy_document" "ssm_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "bastion_ssm_role" {
  name               = "${var.prefix}-bastion-ssm-role"
  assume_role_policy = data.aws_iam_policy_document.ssm_assume.json
}

resource "aws_iam_role_policy_attachment" "bastion_ssm_core" {
  role       = aws_iam_role.bastion_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "bastion_ssm_profile" {
  name = "${var.prefix}-bastion-ssm-profile"
  role = aws_iam_role.bastion_ssm_role.name
}

resource "aws_security_group" "bastion_sg" {
  name   = "${var.prefix}-bastion-sg"
  vpc_id = aws_vpc.main.id

  # No inbound required when using SSM Session Manager
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "bastion" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.bastion_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.bastion_ssm_profile.name

  tags = { Name = "${var.prefix}-bastion" }
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}
