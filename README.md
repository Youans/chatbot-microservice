# PIDIMA Chatbot Platform

A microservices-based chatbot platform consisting of:

- Gateway service (Spring Cloud Gateway + Spring Security WebFlux)
- Chatbot service (Spring Boot WebFlux)
- UI (static HTML/JS)
- Infrastructure as code (Terraform) for AWS (VPC, ECS Fargate, ALB, ECR, ElastiCache Redis, DynamoDB, Service Discovery)

This README covers local development, authentication flow (JWT access + refresh tokens), building & running with Docker, and AWS deployment via Terraform.

---

## Architecture

- **Gateway service** (`gateway-service/`)
  - Routes UI/API traffic, enforces JWT authentication for `/api/**` routes
  - Issues access and refresh tokens under `/auth/*`
  - Proxies `/health` to actuator health internally
  - Maps JWT `roles` claim to `ROLE_*` authorities and supports `@PreAuthorize`
- **Chatbot service** (`chatbot-service/`)
  - Provides chat endpoints (session, message, history)
  - Modular persistence (in-memory or DynamoDB)
  - Pluggable LLM client (OpenAI or Dummy)
- **UI** (`ui-app/`)
  - `login.html` and `index.html` front-end calling the gateway
  - Handles access token storage and refresh on 401 via `/auth/refresh`
- **Infra** (`infra/terraform/`)
  - AWS VPC, subnets, NAT, ALB, ECS, ECR, Redis, DynamoDB, CloudWatch, Service Discovery

---

## Authentication Flow (JWT)

- Login (`POST /auth/login`) returns JSON with `accessToken` and sets an HttpOnly `refresh_token` cookie.
- UI stores `accessToken` in `localStorage` and sends `Authorization: Bearer <token>` to gateway.
- When responses return `401`, UI calls `POST /auth/refresh` (with credentials: include) to rotate refresh cookie and obtain a new `accessToken`, then retries the original request.
- Logout clears refresh cookie on the server and wipes the access token from the browser.

See detailed API docs in `docs/API.md`.

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker + Docker Compose
- Node-capable browser (for UI only)
- (Optional) AWS access for Terraform

---

## Local Development

### 1) Build services

```bash
mvn -f gateway-service/pom.xml clean package -DskipTests
mvn -f chatbot-service/pom.xml clean package -DskipTests
```

### 2) Run via Docker Compose

```bash
docker compose --env-file .env up -d --build
```

- Gateway: http://localhost:18081
- UI: http://localhost:18082/login.html?gateway=http://localhost:18081

### 3) Login (dev credentials)

- Username: `admin`
- Password: `admin`

### 4) Use the UI

- Create session, send message, refresh history
- Health button calls `GET {gateway}/health`

### 5) Troubleshooting

- If `401` occurs on `/api/**`, ensure the UI has an access token in `localStorage` and that `/auth/refresh` is reachable with cookies.
- Confirm CORS allows `http://localhost:18082` and `credentials: include` is used for refresh/logout.

---

## Building Docker images manually

```bash
# Gateway
docker build -t pidima/gateway-service:0.0.1-SNAPSHOT gateway-service

# Chatbot
docker build -t pidima/chatbot-service:0.0.1-SNAPSHOT chatbot-service
```

---

## AWS Deployment with Terraform

This repo contains Terraform to provision:

- VPC, Subnets, NAT, Route Tables
- ALB
- ECS Fargate cluster + services
- ECR repositories for gateway and chatbot
- ElastiCache Redis
- DynamoDB table for chat history
- CloudWatch Log Groups
- Service Discovery (private namespace)

### 1) Backend state on S3

`infra/terraform/versions.tf` is configured to use S3:

- bucket: `chatbot-terraform-state-<id>`
- key: `pidima-chatbot/state/terraform.tfstate`
- region: `eu-central-1`

Run:

```bash
cd infra/terraform
terraform init -upgrade -reconfigure
```

### 2) Optional: Provide OpenAI API key and secret name

Warning: Supplying secrets to Terraform stores them in Terraform state. Ensure S3 state is encrypted and access-controlled.

```bash
export TF_VAR_openai_api_key="<your-openai-key>"
terraform apply \
  -var 'prefix=pidima' \
  -var 'region=eu-central-1' \
  -var 'openai_secret_name=pidima-openai'
```

If you omit `openai_secret_name` or `openai_api_key`, no secret will be created and the chatbot task will not receive OpenAI credentials (it will default to Dummy LLM).

### 3) ECR and images

Terraform creates ECR repos:

- `gateway_ecr_repo_url`
- `chatbot_ecr_repo_url`

Push images (tag "latest" to match ECS task definition):

```bash
aws ecr get-login-password --region eu-central-1 | \
  docker login --username AWS --password-stdin <account>.dkr.ecr.eu-central-1.amazonaws.com

docker tag pidima/gateway-service:0.0.1-SNAPSHOT <gateway_repo_url>:latest

docker tag pidima/chatbot-service:0.0.1-SNAPSHOT <chatbot_repo_url>:latest

docker push <gateway_repo_url>:latest

docker push <chatbot_repo_url>:latest
```

ECS services will pull `latest` and start tasks behind the ALB.

### 4) Outputs

- `alb_dns_name` – the public endpoint for the gateway

---

## Configuration

- `gateway-service` uses `SecurityConfig` to:
  - Permit `/health`, `/actuator/**`, `/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and OPTIONS
  - Require auth for `/api/**`
  - Validate JWT (HS256 secret, issuer, audience from properties)
  - Map `roles` claim to authorities (prefix `ROLE_`)
- `chatbot-service` has pluggable persistence and LLM provider via properties/env

---

## API Reference

See `docs/API.md` for complete request/response examples.

- Auth: `/auth/login`, `/auth/refresh`, `/auth/logout`
- Health: `/health`
- User info: `/me` (secured)
- Chat: `/api/chat/session`, `/api/chat/message`, `/api/chat/history/{sessionId}`

---

## Contributing

- Use feature branches and PRs.
- Add tests for new endpoints.
- Keep `docs/API.md` synchronized with code.

## License

Proprietary – internal project.
