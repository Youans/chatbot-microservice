terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.35"
    }
  }

  backend "s3" {
    bucket  = "chatbot-terraform-state-dml5xuggm8"
    key     = "pidima-chatbot/state/terraform.tfstate"
    region  = "eu-central-1"
    encrypt = true
    # dynamodb_table = "terraform-locks" # optional: add if you create a lock table
  }
}

provider "aws" {
  region = var.region
}
