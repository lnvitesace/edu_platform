# Infrastructure Guide

## Components

- MySQL
- Redis
- Nacos
- RabbitMQ
- Elasticsearch
- Zipkin
- Prometheus
- Grafana
- Loki
- Promtail

## Required Environment

Create `docker/.env` from [docker/.env.example](/Users/rainsfall/Learning/edu_platform/docker/.env.example) and replace all placeholder secrets before startup.

Required sensitive values include:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `RABBITMQ_PASS`
- `JWT_SECRET`
- `INTERNAL_SERVICE_TOKEN`
- `GRAFANA_ADMIN_PASSWORD`

## Startup

```bash
./scripts/start-infra.sh --wait
cd docker
docker compose up -d gateway-service user-service course-service search-service
```

## Exposure Model

- Infrastructure dashboards may publish host ports for local use
- Only `gateway-service` is intended as the application entrypoint
- User, course, and search services stay on the Docker bridge network
