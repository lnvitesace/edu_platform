# ADR-002: Spring Cloud Gateway as the API Gateway

## Status

Accepted

## Context

A microservices architecture needs a unified entry point to handle:

- Request routing to backend services
- Cross-service authentication through JWT validation
- Resilience features such as rate limiting and circuit breaking
- Cross-origin handling through CORS

## Decision

Adopt Spring Cloud Gateway as the API gateway.

**Core responsibilities**

- Routing: dispatch requests to services based on path prefixes
- Authentication: perform global JWT validation and inject user information into headers
- Circuit breaking: protect downstream services with Resilience4j
- Load balancing: integrate with Nacos service discovery

**Route examples**

```yaml
/api/auth/** -> user-service (no authentication)
/api/users/** -> user-service
/api/courses/** -> course-service
```

## Alternatives Considered

| Option | Pros | Cons |
|---|---|---|
| Netflix Zuul | Mature and stable | No longer maintained, Servlet-based |
| Kong | Rich feature set and plugin ecosystem | Extra infrastructure, higher learning cost |
| Nginx | High performance | Weak dynamic routing, poor integration with the Spring ecosystem |
| Spring Cloud Gateway | Reactive, deeply integrated with Spring | Relatively newer |

## Consequences

**Benefits**

- Reactive architecture with strong concurrency performance
- Seamless integration with the Spring Cloud ecosystem
- Declarative route configuration that is easier to maintain
- Built-in support for Resilience4j circuit breakers

**Drawbacks**

- WebFlux-based debugging is more complex
- The reactive programming model has a learning curve

## Technical Implementation

**Circuit breaker configuration**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      courseServiceCircuitBreaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
```

**JWT validation flow**

1. A global filter intercepts the request.
2. Whitelisted paths such as `/auth/**` are allowed through directly.
3. The gateway validates the JWT signature and expiration.
4. It extracts `userId` and `role` and injects them into headers such as `X-User-Id` and `X-User-Role`.
5. Downstream services read user information from the request headers.

## Related Files

- `gateway-service/src/main/resources/application.yml`
- `gateway-service/src/main/java/com/edu/gateway/filter/JwtAuthenticationFilter.java`
- `gateway-service/src/main/java/com/edu/gateway/config/CorsConfig.java`
