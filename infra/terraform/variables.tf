variable "prefix" {
  description = "Resource name prefix"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "eu-central-1"
}

variable "az_count" {
  description = "Number of AZs to use"
  type        = number
  default     = 2
}

variable "vpc_cidr" {
  description = "VPC CIDR"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "List of public subnet CIDRs"
  type        = list(string)
  default     = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "List of private subnet CIDRs"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "allowed_ingress_cidrs" {
  description = "CIDRs allowed to access ALB"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "gateway_image" {
  description = "ECR image URI for gateway"
  type        = string
  default     = ""
}

variable "chatbot_image" {
  description = "ECR image URI for chatbot"
  type        = string
  default     = ""
}

variable "image_tag" {
  description = "Docker image tag to deploy from the ECR repositories"
  type        = string
  default     = "latest"
}

variable "desired_count_gateway" {
  type    = number
  default = 2
}

variable "desired_count_chatbot" {
  type    = number
  default = 2
}

variable "container_cpu" {
  type    = number
  default = 256
}

variable "container_memory" {
  type    = number
  default = 512
}

variable "openai_secret_name" {
  description = "Secrets Manager secret name for OpenAI API key"
  type        = string
  default     = null
}

variable "openai_api_key" {
  description = "OpenAI API key to store in Secrets Manager (warning: will be written to Terraform state)"
  type        = string
  default     = null
  sensitive   = true
}
