# Pidima Gateway Service (Spring Cloud Gateway)

A minimal API gateway in front of the `chatbot-service`, with basic security and health endpoint.

## Features
- Routes `/api/**` to the chatbot service (default `http://localhost:8080`)
- JWT-based auth (OAuth2 Resource Server) for all routes except `/health` and actuator health
- Swagger UI for gateway (not for downstream service)
- Dockerfile for containerization

## Configuration
`src/main/resources/application.yml`:
- `server.port`: 8081 (gateway)
- `spring.cloud.gateway.routes[0].uri`: `${CHATBOT_BASE_URL:http://localhost:8080}`
- JWT settings (override via env):
  - `GATEWAY_JWT_SECRET` (HS256 secret; default `dev-secret`)
  - `GATEWAY_JWT_ISSUER` (default `http://pidima.local`)
  - `GATEWAY_JWT_AUDIENCE` (default `chatbot`)

## Run Locally
In one terminal (chatbot-service):
```bash
cd ../chatbot-service
mvn spring-boot:run
```
In another terminal (gateway-service):
```bash
cd ../gateway-service
mvn spring-boot:run
```

## Example Requests (via Gateway)
```bash
# Health (public)
curl -s http://localhost:8081/health

# Create a JWT (HS256) with issuer and audience
# Example using node 'jsonwebtoken' (or use jwt.io):
# payload: {"iss":"http://pidima.local","aud":"chatbot","sub":"user-123"}
# secret: dev-secret

# Use the token in Authorization header
TOKEN="<your.jwt.token>"

# Create session (auth required)
curl -s -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8081/api/chat/session \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user-123"}'

# Send message (replace SESSION_ID)
curl -s -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8081/api/chat/message \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"SESSION_ID","message":"How do I deploy on AWS?"}'

# Get history
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/chat/history/SESSION_ID
```

## Docker
Build and run gateway:
```bash
docker build -t pidima/gateway-service:latest .
# Ensure chatbot-service is reachable at CHATBOT_BASE_URL (e.g., host.docker.internal or a compose network)
docker run --rm -p 8081:8081 \
  -e CHATBOT_BASE_URL=http://host.docker.internal:8080 \
  -e GATEWAY_JWT_SECRET=dev-secret \
  pidima/gateway-service:latest
```

## docker-compose
At repo root (`/home/youans/CascadeProjects/pidima-chatbot-backend`):
```bash
docker compose up --build
```
Services:
- `chatbot` on port 8080
- `gateway` on port 8081 (JWT-protected)
- `dynamodb-local` on port 8000 (optional; set `APP_PERSISTENCE=dynamodb`)

## Notes
- This is a developer gateway. In production on AWS, prefer Amazon API Gateway + WAF + Cognito as per `docs/architecture.md`. This Spring Gateway can still be useful inside the VPC as an internal aggregator or for local development.
