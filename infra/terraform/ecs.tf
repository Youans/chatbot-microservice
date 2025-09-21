resource "aws_ecs_cluster" "main" {
  name = "${var.prefix}-ecs"
}

# Allow intra-SG access to chatbot on 8080 (gateway task -> chatbot task)
resource "aws_security_group_rule" "ecs_tasks_intra_chatbot" {
  type                     = "ingress"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  security_group_id        = aws_security_group.ecs_tasks_sg.id
  source_security_group_id = aws_security_group.ecs_tasks_sg.id
  description              = "ECS tasks to chatbot port"
}

resource "aws_security_group" "ecs_tasks_sg" {
  name   = "${var.prefix}-ecs-tasks-sg"
  vpc_id = aws_vpc.main.id

  # Egress to anywhere (NAT for internet)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Ingress rules defined via separate aws_security_group_rule resources

  tags = { Name = "${var.prefix}-ecs-tasks-sg" }
}

# Allow ALB to reach gateway task on 8081
resource "aws_security_group_rule" "alb_to_gateway" {
  type                     = "ingress"
  from_port                = 8081
  to_port                  = 8081
  protocol                 = "tcp"
  security_group_id        = aws_security_group.ecs_tasks_sg.id
  source_security_group_id = aws_security_group.alb_sg.id
}

# Service discovery (optional) - skipped for brevity; using SG rules

resource "aws_iam_role" "task_exec" {
  name               = "${var.prefix}-task-exec"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_execution_assume.json
}

resource "aws_iam_role_policy_attachment" "task_exec_attach" {
  role       = aws_iam_role.task_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Gateway Task Definition
resource "aws_ecs_task_definition" "gateway" {
  family                   = "${var.prefix}-gateway"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.container_cpu
  memory                   = var.container_memory
  execution_role_arn       = aws_iam_role.task_exec.arn
  task_role_arn            = aws_iam_role.app_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "gateway"
      image     = format("%s:%s", aws_ecr_repository.gateway.repository_url, var.image_tag)
      essential = true
      portMappings = [{ containerPort = 8081, protocol = "tcp" }]
      environment = [
        { name = "CHATBOT_BASE_URL", value = "http://chatbot.${aws_service_discovery_private_dns_namespace.ns.name}:8080" },
        { name = "REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
        { name = "REDIS_PORT", value = tostring(aws_elasticache_cluster.redis.cache_nodes[0].port) },
        { name = "GATEWAY_JWT_ISSUER", value = "http://pidima.local" },
        { name = "GATEWAY_JWT_AUDIENCE", value = "chatbot" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.gateway.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "gateway"
        }
      }
    }
  ])

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }
}

# Chatbot Task Definition
resource "aws_ecs_task_definition" "chatbot" {
  family                   = "${var.prefix}-chatbot"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.container_cpu
  memory                   = var.container_memory
  execution_role_arn       = aws_iam_role.task_exec.arn
  task_role_arn            = aws_iam_role.app_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "chatbot"
      image     = format("%s:%s", aws_ecr_repository.chatbot.repository_url, var.image_tag)
      essential = true
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      environment = [
        { name = "APP_PERSISTENCE", value = "dynamodb" },
        { name = "APP_LLM_PROVIDER", value = "openai" },
        { name = "APP_LLM_MODEL", value = "gpt-4o-mini" },
        { name = "AWS_REGION", value = var.region }
      ]
      secrets = var.openai_secret_name == null ? [] : [
        {
          name      = "OPENAI_API_KEY"
          valueFrom = aws_secretsmanager_secret_version.openai[0].arn
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.chatbot.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "chatbot"
        }
      }
    }
  ])

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }
}

# Gateway Service (public via ALB)
resource "aws_ecs_service" "gateway" {
  name            = "${var.prefix}-gateway"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.gateway.arn
  desired_count   = var.desired_count_gateway
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.gateway_tg.arn
    container_name   = "gateway"
    container_port   = 8081
  }

  depends_on = [aws_lb_listener.http]
}

# Chatbot Service (internal only)
resource "aws_ecs_service" "chatbot" {
  name            = "${var.prefix}-chatbot"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.chatbot.arn
  desired_count   = var.desired_count_chatbot
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks_sg.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.chatbot.arn
  }
}
