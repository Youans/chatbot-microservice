# System Description – Pidima AI Documentation Assistant Backend

## Architecture Decisions and Rationale
- **Edge and API Layer**: We use `AWS WAF` + `Amazon API Gateway` to provide DDoS protection, request throttling, JWT/OAuth2 auth (via `Amazon Cognito`), and centralized API management. This cleanly separates the public edge from app services and supports custom domains.
- **Compute**: `Amazon ECS on Fargate` is chosen to run the stateless `chatbot-service` (Spring Boot). Fargate removes server management, scales horizontally, and integrates with `CloudWatch` and `X-Ray`. `AWS Lambda` can be used for auxiliary async tasks or webhooks if needed. `EC2` is an alternative when deeper OS control is required.
- **State**: `Amazon DynamoDB` stores chat sessions and messages (high write throughput, predictable low-latency, and serverless scaling). It suits semi-structured chat history well. Optional `Amazon RDS/Aurora` if relational joins/transactions are necessary.
- **Cache**: `Amazon ElastiCache for Redis` caches session context and reduces DB lookups before LLM calls.
- **Async Processing**: `Amazon SQS` decouples long-running tasks (e.g., document ingestion, chunking, and embedding jobs) to protect interactive APIs from spikes.
- **LLM Integration**: Prefer `AWS Bedrock` for enterprise controls, model choice, and VPC Endpoints. Optionally support `OpenAI` with private networking or allowed egress.
- **Storage**: `Amazon S3` stores documents, artifacts, and optionally pre-computed embeddings. Add `OpenSearch Serverless` for vector search on embeddings if needed.
- **Security**: Defense in depth via WAF, Cognito, private subnets for ECS, security groups, KMS encryption for data at rest (DynamoDB, S3, Redis), TLS in transit, and secrets in `AWS Secrets Manager`.
- **Observability**: `CloudWatch` logs and metrics, `X-Ray` tracing, and API Gateway access logs.

## API Design Principles
- **RESTful endpoints** with resource-oriented paths under `/chat` and clear status codes.
- **Validation** using Jakarta Bean Validation with meaningful error payloads (`GlobalExceptionHandler`).
- **Idempotency** for `POST /chat/session` by allowing optional `userId` and treating session creation as a pure generate operation; for message send, clients can add idempotency keys in the future.
- **Documentation** via OpenAPI/Swagger UI for discoverability.

## Data Flow
1. Client calls `POST /chat/session` via API Gateway (authorized by Cognito). Request reaches `ALB` -> `ECS Fargate` service.
2. Service creates a new session ID, seeds system message, and stores it in `DynamoDB` (in this template, in-memory map).
3. Client sends `POST /chat/message`. Service appends user message to history (cache + DB), invokes LLM client (Bedrock/OpenAI) with limited context, obtains reply, stores it, returns response.
4. Client reads `GET /chat/history/{sessionId}`. Service retrieves messages (from cache, fallback to DB), returns list.
5. Logs and traces are emitted to CloudWatch/X-Ray.

## Scalability Considerations
- **Stateless service** enables horizontal scaling behind ALB. Scale on CPU/memory or ALB target response time.
- **DynamoDB auto scaling** on table throughput. Employ partition keys like `tenantId#sessionId` to spread write load.
- **Redis** to reduce read amplification and lower tail latency for hot sessions.
- **SQS** to absorb spikes from background processing (ingestion, indexing). Separate worker service consumes queue.
- **Chunked context windows** and prompt caching to control LLM token usage.

## Security Approach
- **Authentication/Authorization**: Cognito User Pools / Identity Pools; JWT validation at API Gateway and optionally in the service.
- **Network**: Private subnets for ECS tasks; VPC endpoints for DynamoDB/Bedrock; egress limited by NAT Gateway and security groups.
- **Data Protection**: KMS encryption at rest (DynamoDB, S3, Redis), TLS in transit. Secrets in Secrets Manager with rotation policies.
- **Operational Security**: IAM least privilege for tasks; parameterize environment via SSM; audit logs in CloudWatch; WAF rules against OWASP Top 10.

## Cost Optimization
- Prefer serverless or managed services: Fargate on-demand, DynamoDB on-demand capacity for variable traffic.
- Use **S3 + Glacier** tiers for archival docs.
- Right-size Redis nodes; enable auto-scaling.
- Enable CloudWatch log retention policies.
- Prefer Bedrock provisioned throughput only when needed; otherwise, on-demand invocations.
- Caching and prompt truncation to reduce LLM token costs.

## Future Enhancements
- Swap in persistent storage (DynamoDB repository) and Redis cache implementation.
- Add vector search (OpenSearch Serverless) and RAG pipeline with S3/Lambda indexing.
- Add rate limiting (API Gateway usage plans) and tenant-level quotas.
- Implement streaming responses (Server-Sent Events/WebSockets) via API Gateway + WebSocket API.
