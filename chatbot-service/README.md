# Pidima Chatbot Service (Spring Boot)

AI-powered documentation assistant backend demonstrating a scalable architecture and minimal microservice implementation.

## Features
- REST endpoints:
  - `POST /chat/session` – create a new chat session
  - `POST /chat/message` – send message and get assistant reply
  - `GET /chat/history/{sessionId}` – retrieve conversation history
  - `GET /health` – health check
- Request validation, error handling, CORS configuration
- Basic logging (Log4j2) and configuration via `application.yml`
- OpenAPI/Swagger UI at `/swagger-ui/index.html`
- Unit tests with MockMvc
- Docker containerization

## Project Structure
```
chatbot-service/
├── src/
│   ├── main/
│   │   ├── java/com/pidima/chatbot/
│   │   │   ├── api/                # Controllers
│   │   │   ├── config/             # App configuration
│   │   │   ├── models/             # DTOs and model classes
│   │   │   └── services/           # Chat and LLM service
│   │   └── resources/
│   │       └── application.yml
│   └── test/java/com/pidima/chatbot/api/  # Tests
├── docs/
│   ├── architecture.md             # AWS architecture diagram (Mermaid)
│   └── system-description.md       # Architecture and design decisions
├── pom.xml
├── Dockerfile
└── README.md
```

## Prerequisites
- Java 17+
- Maven 3.9+
- Docker (optional for container run)

## How to Run (Local)
```bash
mvn spring-boot:run
```
Then open:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/health

## Example cURL
```bash
# Create session
curl -s -X POST http://localhost:8080/chat/session \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user-123"}'

# Send message (replace SESSION_ID)
curl -s -X POST http://localhost:8080/chat/message \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"SESSION_ID","message":"How do I deploy on AWS?"}'

# Get history
curl -s http://localhost:8080/chat/history/SESSION_ID
```

## Run Tests
```bash
mvn test
```

## Build JAR
```bash
mvn -DskipTests package
```
The JAR is produced at `target/chatbot-service-0.0.1-SNAPSHOT.jar`.

## Run with Docker
```bash
# Build image
docker build -t pidima/chatbot-service:latest .

# Run container
docker run --rm -p 8080:8080 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  pidima/chatbot-service:latest
```

## Configuration
Edit `src/main/resources/application.yml`:
- `app.cors-origins` – allowed origins for CORS
- `app.llm-provider` – `dummy` by default; set to `openai` (or `bedrock` when implemented)
- `app.llm-model` – model identifier (e.g., `gpt-4o-mini`, `gpt-4o`) when using OpenAI
- `app.persistence` – `memory` (default) or `dynamodb`
- `app.openai-api-key` – or set env var `OPENAI_API_KEY`
- `app.openai-base-url` – defaults to `https://api.openai.com/v1`

## Notes on Production Integration
- Replace `DummyLlmClient` with provider-specific clients (e.g., AWS Bedrock, OpenAI)
- Persist session history using a datastore (e.g., Amazon DynamoDB or RDS). The current template uses in-memory storage for simplicity.
- Add authentication/authorization (Amazon Cognito / OAuth2 / JWT), request throttling, and audit logging.

### Enable DynamoDB Persistence
1) Set in `application.yml` or env:
```
app:
  persistence: dynamodb
```
2) Create a DynamoDB table named `chat_messages` with:
- Partition (HASH) key: `sessionId` (String)
- Sort (RANGE) key: `ts` (Number; epoch millis)

3) Ensure AWS credentials and region are available (e.g., environment, instance role). Optionally set `DYNAMODB_ENDPOINT` for LocalStack.

### Enable OpenAI Client
1) Set in `application.yml` or env:
```
app:
  llm-provider: openai
  llm-model: gpt-4o-mini
  openai-api-key: ${OPENAI_API_KEY}
```
2) Export `OPENAI_API_KEY` in your environment.
3) Optional: set `app.openai-base-url` for proxies or Azure OpenAI-compatible endpoints.

Refer to `docs/` for the AWS architecture and system description.
