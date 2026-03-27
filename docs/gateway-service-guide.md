# Gateway Service Guide

## Responsibility

`gateway-service` is the public HTTP entrypoint for the platform. It handles:

- JWT verification
- route forwarding to backend services
- rate limiting
- circuit breaking
- cross-origin policy for the frontend

## Public Surface

- Host port: `8080`
- Internal routes resolve through service discovery
- Docker Compose publishes only the gateway; downstream services stay internal

## Trust Boundary

- `gateway-service` forwards `X-User-Id` and `X-User-Role` after JWT validation
- downstream services should be treated as internal-only workloads
- `user-service` internal APIs are separately protected with `INTERNAL_SERVICE_TOKEN`

## Local Validation

```bash
cd gateway-service
mvn test
```
