# AWS Architecture Diagram – Pidima AI Documentation Assistant

Below is a high-level architecture using AWS managed services. This design supports multi-tenant usage, persistence of chat history, LLM integration, security layers, observability, and scalability for enterprise workloads.

```mermaid
flowchart LR
    subgraph Internet
        U[Users / Frontends]
    end

    subgraph AWS[Amazon Web Services]
        WAF[AWS WAF]
        APIGW[Amazon API Gateway]
        COGNITO[Amazon Cognito]
        
        subgraph VPC[VPC]
            subgraph Pub[Public Subnets]
                ALB[Application Load Balancer]
            end
            subgraph Priv[Private Subnets]
                ECS[ECS Fargate Service: chatbot-service]
                ECR[(Amazon ECR)]
                REDIS[(Amazon ElastiCache Redis)]
                SQS[(Amazon SQS)]
                XRay[AWS X-Ray]
            end
        end

        CW[Amazon CloudWatch Logs & Metrics]
        SM[Secrets Manager]
        PARAM[SSM Parameter Store]
        DDB[(Amazon DynamoDB: chat sessions & messages)]
        RDS[(Amazon RDS/Aurora - optional)]
        S3[(Amazon S3: documents & attachments)]
        
        subgraph LLM[LLM Providers]
            BR[AWS Bedrock (preferred)]
            OAI[OpenAI API (alt)]
        end
    end

    U -->|HTTPS| WAF --> APIGW
    APIGW -->|JWT/OAuth2| COGNITO
    APIGW --> ALB --> ECS

    ECS -->|Read/Write| DDB
    ECS -->|Cache sessions| REDIS
    ECS -->|Async tasks| SQS
    ECS -->|Fetch secrets| SM
    ECS -->|Config| PARAM
    ECS -->|LLM calls| BR
    ECS -->|LLM calls| OAI
    ECS -->|Artifacts| S3

    ECS -. traces .-> XRay
    ECS -. logs .-> CW
    APIGW -. access logs .-> CW
```

## Notes
- API Gateway + WAF provide edge security, throttling, and authentication/authorization (Cognito). Optionally, ALB can terminate TLS; API Gateway integrates with ALB via VPC Link or HTTP integration.
- ECS Fargate runs the stateless Spring Boot `chatbot-service` across multiple Availability Zones. Auto Scaling based on CPU/RAM/ALB target response time.
- DynamoDB stores chat sessions and messages (write-optimized, scalable, low-latency). Optionally, RDS/Aurora is used when relational features or SQL analytics are required.
- Redis (ElastiCache) caches hot session contexts and recent history to speed up LLM prompts and reduce DB reads.
- SQS buffers async jobs (e.g., document ingestion, vector indexing, long-running enrichment). A separate worker service can process the queue.
- Secrets Manager and SSM Parameter Store hold API keys (e.g., Bedrock/OpenAI), DB strings, and feature flags.
- LLM integration prefers AWS Bedrock for enterprise controls, with OpenAI as an alternate provider.
- Observability via CloudWatch Logs, Metrics, and X-Ray tracing.
- S3 stores documentation corpora, embeddings dumps, or generated artifacts. Combine with Amazon OpenSearch Serverless for vector search if needed.
