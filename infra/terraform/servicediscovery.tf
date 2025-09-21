resource "aws_service_discovery_private_dns_namespace" "ns" {
  name        = "svc.local"
  description = "Private namespace for ECS services"
  vpc         = aws_vpc.main.id
}

resource "aws_service_discovery_service" "chatbot" {
  name = "chatbot"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.ns.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "WEIGHTED"
  }
  health_check_custom_config { failure_threshold = 1 }
}
