# ADR-006: Resilience4j Circuit Breaking and Rate Limiting

## Status

Accepted

## Context

In a microservices architecture, service-to-service calls can fail because of network issues or downstream outages, which can trigger cascading failures. The system needs:

- Fast failure to avoid resource exhaustion
- Automatic recovery to reduce manual intervention
- Graceful degradation so core functionality remains available

## Decision

Use Resilience4j at the API Gateway layer to provide circuit breaker protection.

**Circuit breaker configuration**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      courseServiceCircuitBreaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
```

**Circuit breaker state machine**

```text
CLOSED (normal) ---- failure rate exceeds threshold ----> OPEN (tripped)
    ^                                                       |
    |                                                       |
    |                                                  wait timeout
    |                                                       |
    +------------- probe succeeds ---- HALF_OPEN <----------+
                      |
                      |
                probe fails
                      |
                      v
                     OPEN
```

## Alternatives Considered

| Option | Pros | Cons |
|---|---|---|
| Netflix Hystrix | Mature, well documented | No longer maintained |
| Resilience4j | Lightweight, functional style, strong Spring integration | Simpler feature set |
| Sentinel | Powerful, Alibaba-backed | More heavyweight |

## Consequences

**Benefits**

- Prevents cascading failures
- Fails fast and protects system resources
- Recovers automatically and reduces operational intervention
- Integrates well with Spring Cloud Gateway

**Drawbacks**

- Parameter tuning requires production data
- User experience degrades while a circuit is open

## Fallback Strategy

**Gateway-level fallback**

```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("course-service", r -> r
            .path("/api/courses/**")
            .filters(f -> f
                .circuitBreaker(c -> c
                    .setName("courseServiceCircuitBreaker")
                    .setFallbackUri("forward:/fallback/courses")))
            .uri("lb://course-service"))
        .build();
}
```

**Fallback response**

```json
{
  "code": 503,
  "message": "Course service is temporarily unavailable. Please try again later.",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Monitoring Metrics

Resilience4j exposes Prometheus metrics such as:

- `resilience4j_circuitbreaker_state`
- `resilience4j_circuitbreaker_calls_total`
- `resilience4j_circuitbreaker_failure_rate`

## Related Files

- `gateway-service/src/main/resources/application.yml`
- `gateway-service/src/main/java/com/edu/gateway/controller/FallbackController.java`
