# Docs Index

Current operational documentation for this repository:

- [README.md](/Users/rainsfall/Learning/edu_platform/README.md): top-level setup and repository posture
- [docker/README.md](/Users/rainsfall/Learning/edu_platform/docker/README.md): Docker and local environment bootstrap
- [course-service.md](/Users/rainsfall/Learning/edu_platform/docs/course-service.md): course domain service notes
- [gateway-service-guide.md](/Users/rainsfall/Learning/edu_platform/docs/gateway-service-guide.md): gateway responsibilities and trust boundaries
- [infrastructure-guide.md](/Users/rainsfall/Learning/edu_platform/docs/infrastructure-guide.md): infrastructure topology and required secrets
- [monitoring-guide.md](/Users/rainsfall/Learning/edu_platform/docs/monitoring-guide.md): Prometheus, Grafana, and Loki
- [phase4-containerization.md](/Users/rainsfall/Learning/edu_platform/docs/phase4-containerization.md): current containerization layout

Runtime baseline:

- Docker Compose containers run the Java services on `Java 25`.
- Any local Maven workflow such as `./scripts/start-all.sh`, `mvn test`, or `mvn spring-boot:run` also requires the host `java` and `mvn` to target `Java 25`.

Historical research or narrative material in this directory should not override the files above when they disagree.
