# Monitoring Guide

## Stack

- Prometheus for metrics collection
- Grafana for dashboards
- Loki plus Promtail for container logs
- Zipkin for tracing

## Access

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`
- Loki: `http://localhost:3100`
- Zipkin: `http://localhost:9411`

Grafana credentials come from `docker/.env`. Do not rely on any hardcoded default password in documentation.

## Scrape Targets

Prometheus scrapes actuator metrics from internal service names on the Docker network, including:

- `gateway-service`
- `user-service`
- `course-service`
- `search-service`

## Validation

```bash
./tests/infra/test-runner.sh
```
