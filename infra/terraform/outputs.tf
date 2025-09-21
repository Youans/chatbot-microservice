output "alb_dns_name" {
  value       = aws_lb.public.dns_name
  description = "Public ALB DNS name for the gateway"
}

output "gateway_ecr_repo_url" {
  value       = aws_ecr_repository.gateway.repository_url
  description = "ECR repository URL for gateway"
}

output "chatbot_ecr_repo_url" {
  value       = aws_ecr_repository.chatbot.repository_url
  description = "ECR repository URL for chatbot"
}

output "ecs_cluster_name" {
  value       = aws_ecs_cluster.main.name
  description = "ECS Cluster name"
}

output "dynamodb_table_name" {
  value       = aws_dynamodb_table.chat_messages.name
  description = "DynamoDB table name for chat messages"
}

output "redis_endpoint" {
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
  description = "ElastiCache Redis endpoint"
}
