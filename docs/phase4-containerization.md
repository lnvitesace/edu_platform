# Containerization Layout

## Included Service Images

- `gateway-service`
- `user-service`
- `course-service`
- `search-service`

Each service Dockerfile builds against Java 25 and is paired with a `.dockerignore` to reduce build context size.

## Compose Posture

- sensitive values are required from `docker/.env`
- only the gateway is exposed as the application ingress
- service-to-service communication stays on the internal bridge network
- observability components run alongside the application stack for local operations

## Validation

```bash
./tests/infra/test-runner.sh
```
