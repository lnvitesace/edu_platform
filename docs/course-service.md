# Course Service

## Responsibility

`course-service` owns course metadata, categories, chapters, lessons, enrollments, and learning progress.

## Runtime

- Service port: `8002`
- Discovery name: `course-service`
- Backing stores: MySQL, Redis, RabbitMQ
- Java version: 21

## Integration

- Publishes course events to RabbitMQ for `search-service`
- Calls `user-service` internal API through OpenFeign
- Authenticates that internal call with `X-Internal-Service-Token`

## Security

- Public traffic should arrive through `gateway-service`
- Category write operations are restricted to admin users
- Secrets are injected from environment variables

## Local Validation

```bash
cd course-service
mvn test
```
