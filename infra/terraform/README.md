# Pidima Chatbot Infra (Terraform on AWS)

This Terraform stack provisions a production-ready baseline for the chatbot platform on AWS using ECS Fargate, ALB, DynamoDB, ElastiCache (Redis), CloudWatch, ECR, and Secrets Manager.

High-level:
- VPC with public and private subnets across two AZs
- Internet Gateway + NAT Gateway
- Application Load Balancer (public)
- ECS Fargate Cluster and Services
  - `gateway-service` exposed via ALB
  - `chatbot-service` internal (only reachable from gateway/within VPC)
- ECR repositories for images
- DynamoDB table `chat_messages` (PK: sessionId, SK: ts)
- ElastiCache Redis (rate limiting)
- CloudWatch Log Groups
- IAM roles and least-privileged policies
- Secrets Manager secret for `OPENAI_API_KEY`

## Prerequisites
- Terraform >= 1.5
- AWS credentials configured
- Choose a unique resource prefix and region

## Layout
```
infra/terraform/
├── README.md
├── main.tf
├── variables.tf
├── outputs.tf
├── vpc.tf
├── alb.tf
├── ecs.tf
├── iam.tf
├── ecr.tf
├── dynamodb.tf
├── redis.tf
├── cloudwatch.tf
├── secrets.tf
├── examples/
│   └── dev.tfvars
```

## Quick Start
```bash
cd infra/terraform
terraform init
terraform plan -var-file=examples/dev.tfvars
terraform apply -var-file=examples/dev.tfvars
```

## Build & Push Images
After `terraform apply` output will include ECR repo URIs. Build and push your images:
```bash
# Authenticate to ECR
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account>.dkr.ecr.<region>.amazonaws.com

# Build & push gateway
docker build -t gateway-service:latest ../../gateway-service
docker tag gateway-service:latest <gateway_ecr_repo_url>:latest
docker push <gateway_ecr_repo_url>:latest

# Build & push chatbot
docker build -t chatbot-service:latest ../../chatbot-service
docker tag chatbot-service:latest <chatbot_ecr_repo_url>:latest
docker push <chatbot_ecr_repo_url>:latest
```

Then update the running ECS services to pick up `:latest` (or use CI/CD to push with semantic versions and deploy via CodeDeploy or rolling update).

## Secrets
Set your OpenAI API key into Secrets Manager after apply:
```bash
aws secretsmanager put-secret-value \
  --region <region> \
  --secret-id <prefix>-openai-api-key \
  --secret-string '{"OPENAI_API_KEY":"sk-..."}'
```

## Notes
- For production, add WAF + AWS Managed Rules on ALB, or prefer Amazon API Gateway + WAF + Cognito at the edge as per `docs/architecture.md`.
- To run chatbot service in public via ALB directly, add a listener rule and target group mapping. Current setup exposes only the gateway publicly; chatbot runs in private subnets.
- Adjust ECS service desired counts and auto scaling policies based on load.
