# Example tfvars for a dev environment
prefix = "pidima-dev"
region = "eu-central-1"

# Replace with your actual ECR repo image URIs (after terraform apply outputs them)
# Or pre-create ECR repos and set these now
gateway_image = "123456789012.dkr.ecr.eu-central-1.amazonaws.com/pidima-dev-gateway:latest"
chatbot_image = "123456789012.dkr.ecr.eu-central-1.amazonaws.com/pidima-dev-chatbot:latest"

desired_count_gateway = 2
desired_count_chatbot = 2

container_cpu    = 256
container_memory = 512

# Optional: create and wire a Secrets Manager secret to pass OPENAI_API_KEY into the chatbot task
# openai_secret_name = "pidima-dev-openai-api-key"
