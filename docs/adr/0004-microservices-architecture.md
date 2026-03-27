# ADR-004: Microservices Architecture Design

## Status

Accepted

## Context

The online education platform needs to support:

- Parallel development across multiple teams
- Independent deployment and scaling for each business module
- Flexible technology choices
- Fault isolation

## Decision

Adopt a microservices architecture and split services by business domain.

**Service split**

| Service | Port | Responsibility |
|---|---:|---|
| gateway-service | 8080 | API gateway, routing, authentication, rate limiting |
| user-service | 8001 | User authentication, authorization, profile management |
| course-service | 8002 | Course, chapter, and lesson management |
| learning-service | 8003 | Learning progress and favorites |

**Communication patterns**

- Synchronous: REST APIs for service-to-service calls
- Asynchronous: RabbitMQ for event-driven integration

**Data management**

- One database per service
- Services obtain external data through APIs instead of direct database access

## Alternatives Considered

| Option | Pros | Cons |
|---|---|---|
| Monolith | Simple, high development efficiency | Poor scalability, tightly coupled deployment |
| SOA | Service reuse | Heavy ESB, over-engineered |
| Microservices | Independent deployment, flexible technology choices | Higher distributed-system complexity |
| Serverless | Elastic scaling, no server management | Cold starts, vendor lock-in |

## Consequences

**Benefits**

- Services can be deployed independently for faster iteration
- High-load services such as `course-service` can scale independently
- Fault isolation limits the blast radius of a single service failure
- Team autonomy improves parallel development

**Drawbacks**

- Distributed transactions become more complex
- Service-to-service communication adds latency
- Operational complexity increases
- Data consistency becomes harder

## Service Boundary Principles

1. **Single responsibility**: each service focuses on one business domain.
2. **High cohesion, low coupling**: internals stay tight, inter-service dependencies stay loose.
3. **Independent database**: avoid data-layer coupling.
4. **API-first**: define the contract before implementation.

## Service Dependency Model

```text
                    +-------------+
                    |   Gateway   |
                    +------+------+ 
                           |
         +-----------------+-----------------+
         |                 |                 |
         v                 v                 v
   +-----------+     +-----------+     +-----------+
   |   User    |     |  Course   |     | Learning  |
   |  Service  |     |  Service  |     |  Service  |
   +-----------+     +-----+-----+     +-----+-----+
                           |                 |
                           +--------+--------+
                                    |
                                    v
                         +---------------------+
                         |      RabbitMQ       |
                         |  Event Publishing   |
                         +---------------------+
```

## Related Files

- `gateway-service/` - API gateway
- `user-service/` - user service
- `course-service/` - course service
- `docker/docker-compose.yml` - infrastructure orchestration
