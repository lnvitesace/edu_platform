# ADR-003: Nacos for Service Registration and Discovery

## Status

Accepted

## Context

The microservices architecture needs to solve:

- Dynamic service instance registration and discovery
- Centralized configuration management
- Service health checks
- Environment-specific configuration isolation

## Decision

Adopt Alibaba Nacos as both the service registry and the configuration center.

**Service registration**

- Each microservice registers itself with Nacos on startup
- Supports isolation through service groups and namespaces
- Heartbeat detection is used to keep instance health status up to date

**Service discovery**

- The gateway routes by service name instead of hard-coded IP addresses
- Load-balancing strategies can be configured

**Configuration management**

- Reserve support for dynamic configuration refresh
- Support environment-specific configuration isolation

## Alternatives Considered

| Option | Pros | Cons |
|---|---|---|
| Netflix Eureka | Mature, native Spring Cloud support | Service discovery only, requires Config Server for config management |
| Consul | Broad feature set, supports KV storage | More complex deployment, tied to the HashiCorp ecosystem |
| Zookeeper | Strong consistency, widely adopted in large systems | CP model, more operational complexity |
| Nacos | Combines service discovery and configuration management | Smaller community compared with more global alternatives |

## Consequences

**Benefits**

- One system covers both service discovery and configuration management
- Web console improves operations and debugging
- Supports both DNS and RPC discovery patterns
- AP-oriented design favors availability

**Drawbacks**

- Ecosystem strength is greater in the domestic market than internationally
- Requires an extra Nacos Server deployment

## Technical Implementation

**Service registration configuration**

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: edu-platform
        group: DEFAULT_GROUP
```

**Gateway route integration**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: course-service
          uri: lb://course-service
          predicates:
            - Path=/api/courses/**
```

## Deployment Topology

```text
+-----------------------------------------+
|              Nacos Server               |
|            (localhost:8848)             |
+-----------------------------------------+
        ^           ^           ^
   register     register     register
   heartbeat    heartbeat    heartbeat
        |           |           |
+-------+---+ +-----+-----+ +---+-------+
|  Gateway  | |   User    | |  Course   |
|   :8080   | |  :8001    | |  :8002    |
+-----------+ +-----------+ +-----------+
```

## Related Files

- `user-service/src/main/java/com/edu/platform/config/NacosConfig.java`
- `course-service/src/main/java/com/edu/course/config/NacosConfig.java`
- `gateway-service/src/main/resources/application.yml`
