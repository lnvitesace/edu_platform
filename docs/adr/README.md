# Architecture Decision Records (ADR)

This directory records the key architecture decisions for the online education platform.

## What Is an ADR?

An ADR, or Architecture Decision Record, is a lightweight document format used to capture an important architectural decision, the context behind it, the alternatives considered, and its consequences.

## ADR Index

| ID | Title | Status |
|---|---|---|
| [ADR-001](0001-jwt-authentication.md) | JWT Dual-Token Authentication | Accepted |
| [ADR-002](0002-spring-cloud-gateway.md) | Spring Cloud Gateway as the API Gateway | Accepted |
| [ADR-003](0003-nacos-service-discovery.md) | Nacos for Service Registration and Discovery | Accepted |
| [ADR-004](0004-microservices-architecture.md) | Microservices Architecture Design | Accepted |
| [ADR-005](0005-database-per-service.md) | Database per Service | Accepted |
| [ADR-006](0006-resilience4j-circuit-breaker.md) | Resilience4j Circuit Breaking and Rate Limiting | Accepted |

## ADR Template

```markdown
# ADR-XXX: Title

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
Describe the decision context and the problem to solve.

## Decision
Describe the decision in detail.

## Alternatives Considered
List the other options that were evaluated and their trade-offs.

## Consequences
Describe the positive and negative impacts of the decision.
```

## References

- [Documenting Architecture Decisions - Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [ADR GitHub Organization](https://adr.github.io/)
