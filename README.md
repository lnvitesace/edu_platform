# Edu Platform

A full-stack microservices example for an online education platform. The repository includes authentication, course management, enrollment and progress flows, search, service governance, observability, and end-to-end verification.

## Overview

**Stack**

- Backend: `Java 25`, `Spring Boot 4.0.1`, `Spring Cloud 2025.1.0`
- Frontend: `React 19`, `Vite 6`, `React Router 7`
- Infrastructure: `MySQL 8.4`, `Redis 8.4`, `RabbitMQ 4.2`, `Elasticsearch 9.2`, `Nacos 3.1`
- Observability: `Prometheus`, `Grafana`, `Loki`, `Zipkin`
- Testing: Spring Boot tests, infrastructure validation scripts, `Playwright`

**Main capabilities**

- User registration, login, JWT-based authentication, and profile management
- Course, category, chapter, and lesson management
- Enrollment and learning progress flows
- Course search powered by Elasticsearch
- API Gateway for unified entry, rate limiting, circuit breaking, and routing
- Event-driven search index synchronization
- Metrics, centralized logs, tracing, and E2E verification

## Architecture

```text
+--------------------+        +------------------------------+
| Browser / Client   | -----> | web-app                      |
|                    |        | React 19 + Vite             |
+--------------------+        +---------------+--------------+
                                                |
                                                v
                                +---------------+--------------+
                                | gateway-service              |
                                | JWT / Rate Limit / Routing   |
                                | Circuit Breaker              |
                                +---+-------------+------------+
                                    |             |
                   +----------------+             +----------------+
                   |                                                 |
                   v                                                 v
      +------------+-------------+                     +-------------+------------+
      | Service Registry / Cache |                     | Observability            |
      | Nacos                    |                     | Prometheus -> Grafana    |
      | Redis                    |                     | Loki                     |
      +--------------------------+                     | Zipkin                   |
                                                       +--------------------------+

                                    |
            +-----------------------+-----------------------+
            |                       |                       |
            v                       v                       v
+-----------+-----------+ +---------+-----------+ +---------+-----------+
| user-service          | | course-service      | | search-service      |
| Auth & User           | | Course / Enrollment | | Search Read Model   |
+-----+-----------+-----+ | Progress            | +-----+-----------+---+
      |           |       +-----+---------+-----+       |           |
      |           |             |         |             |           |
      v           v             v         v             v           v
+-----+---+   +---+-----+   +---+-----+ +-+--------+ +--+------+ +--+-----+
| MySQL   |   | Redis   |   | MySQL   | | RabbitMQ | | RabbitMQ | | Elastic |
+---------+   +---------+   +---------+ +----------+ +---------+ | search  |
                                                                  +--------+
```

The system boundary is provided by `gateway-service`. Frontend clients and external consumers should go through the gateway first. The business services are not intended to be the public API boundary. That is also reflected in `docker/docker-compose.yml`: the gateway is published on host port `8080`, while the other application services primarily stay on the internal container network.

## Repository Layout

```text
.
├── web-app/           React + Vite frontend
├── gateway-service/   API Gateway: JWT, rate limiting, circuit breaking, routing
├── user-service/      User and authentication service
├── course-service/    Course, enrollment, and progress service
├── search-service/    Search service consuming course events into Elasticsearch
├── docker/            Docker Compose, infrastructure config, observability stack
├── scripts/           Start/stop and log scripts
├── e2e/               Playwright end-to-end tests and seed data
├── tests/infra/       Infrastructure validation scripts
└── docs/              Current operational and architecture docs
```

Suggested reading order:

1. `README.md`
2. `docker/README.md`
3. `docs/README.md`

## Quick Start

### Option A: Application stack with Docker Compose

Use this if you want to experience the repository quickly or run the backend services, infrastructure, and observability stack with minimal setup.

**Prerequisites**

- Docker Desktop or Docker Engine with Compose
- Available ports: `3001`, `3306`, `6379`, `8080`, `8848`, `9848`, `5672`, `15672`, `9200`, `9411`

**Steps**

```bash
cp docker/.env.example docker/.env
```

Edit `docker/.env` and replace at least these placeholder values:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `RABBITMQ_PASS`
- `JWT_SECRET`
- `INTERNAL_SERVICE_TOKEN`
- `GRAFANA_ADMIN_PASSWORD`

Then start the stack:

```bash
./scripts/start-infra.sh --build --wait
```

This command starts the full Compose topology defined in `docker/docker-compose.yml`, including:

- Infrastructure: MySQL, Redis, Nacos, RabbitMQ, Elasticsearch, Zipkin
- Application containers: gateway-service, user-service, course-service, search-service
- Observability components: Prometheus, Grafana, Loki, Promtail

**Common entry points**

- API Gateway: `http://localhost:8080`
- Nacos: `http://localhost:8848/nacos`
- RabbitMQ: `http://localhost:15672`
- Elasticsearch: `http://localhost:9200`
- Zipkin: `http://localhost:9411`
- Grafana: `http://localhost:3001`

`docker/docker-compose.yml` does not currently include a `web-app` container. If you want the browser UI, start the frontend locally in a separate terminal:

```bash
cd web-app
npm install
npm run dev
```

Stop the environment:

```bash
./scripts/stop-infra.sh
```

Remove volumes as well:

```bash
./scripts/stop-infra.sh --clean
```

### Option B: Local mixed development

Use this if you want to modify code and debug services locally. This mode starts dependencies in Docker, then runs the backend services and frontend on the host machine.

**Additional prerequisites**

- `java` and `mvn` must both target `Java 25`
- `node`, `npm`, `python3`, `curl`, and `nc` must be available

Check versions:

```bash
java -version
mvn -version
node -v
npm -v
```

Start everything:

```bash
./scripts/start-all.sh
```

This script will:

- Start dependency containers in Docker
- Run `gateway-service`, `user-service`, `course-service`, and `search-service` on the host
- Run `web-app` on the host
- Automatically seed demo data if `e2e/node_modules` is already installed

Default entry points:

- Frontend: `http://localhost:3000`
- API Gateway: `http://localhost:8080`
- Grafana: `http://localhost:3001`

Stop local services:

```bash
./scripts/stop-all.sh
```

## Running Individual Parts

If you do not want to use the wrapper scripts, you can start pieces manually.

**Backend services**

```bash
cd user-service && mvn spring-boot:run
cd course-service && mvn spring-boot:run
cd gateway-service && mvn spring-boot:run
cd search-service && mvn spring-boot:run
```

**Frontend**

```bash
cd web-app
npm install
npm run dev
```

**E2E**

```bash
cd e2e
npm install
npm run seed
npm test
```

## Ports

| Component | Port |
|---|---:|
| web-app dev server | `3000` |
| Grafana | `3001` |
| gateway-service | `8080` |
| user-service | `8001` |
| course-service | `8002` |
| search-service | `8005` |
| MySQL | `3306` |
| Redis | `6379` |
| Nacos | `8848`, `9848` |
| RabbitMQ | `5672`, `15672` |
| Elasticsearch | `9200` |
| Zipkin | `9411` |
| Prometheus | `9090` |
| Loki | `3100` |

## Environment Notes

- There is no root-level `.env`. Runtime environment variables primarily live in `docker/.env`.
- `./scripts/start-infra.sh` validates that `docker/.env` exists and that key secrets are not left as placeholders.
- `./scripts/start-all.sh` reads `docker/.env` and rewrites container-network addresses to host addresses for locally running Maven services.
- The current Java runtime baseline is `Java 25`. If `java` and `mvn` do not agree, `start-all.sh` will fail early.

## Testing

**Backend tests**

```bash
cd user-service && mvn test
cd course-service && mvn test
cd gateway-service && mvn test
cd search-service && mvn test
```

**Frontend lint**

```bash
cd web-app && npm run lint
```

**Infrastructure validation**

```bash
./tests/infra/test-runner.sh --quick
./tests/infra/test-runner.sh --full
```

**End-to-end tests**

```bash
cd e2e
npm install
npm run seed
npm test
```

Playwright uses `http://localhost:3000` as its default `baseURL`, so make sure the frontend and the required backend services are reachable before running E2E tests.

## Service Notes

### gateway-service

- Unified system entry point
- Built with Spring Cloud Gateway WebFlux
- Provides JWT validation, Redis-backed rate limiting, and Resilience4j circuit breaking
- Routes are mainly defined in Java DSL, not YAML

### user-service

- Handles registration, login, user management, and authentication-domain logic
- Uses MySQL, Redis, Spring Security, and JWT
- Exposes auth-facing APIs and supports internal service authentication

### course-service

- Handles courses, categories, enrollment, and learning progress
- Uses MySQL, Redis, RabbitMQ, OpenFeign, and LoadBalancer
- Serves as one of the main transactional write-path services

### search-service

- Implements the search read model
- Uses Elasticsearch and RabbitMQ
- Updates indexes by consuming course events instead of querying the transactional database directly

## Development Notes

- `docker/docker-compose.yml` is the accurate source of truth for the current runtime topology. Do not trust stale prose over the Compose file.
- `scripts/start-infra.sh` is slightly misleading by name. It currently starts the full Compose stack, not just databases and caches.
- `scripts/start-all.sh` uses a different model: dependencies in Docker, application services and frontend as local processes.
- The local frontend proxy lives in `web-app/vite.config.js`, and API access is centralized in `web-app/src/services/api.js`. If you change auth, local proxying, or forwarded user headers, inspect both.
- Gateway routes are primarily assembled in `gateway-service/src/main/java/com/edu/gateway/config/GatewayConfig.java`.

## Deployment Considerations

This repository is closer to a complete local development and demonstration environment than to a production-ready deployment template. Before deploying it seriously, at minimum you should:

- Replace all example secrets in `docker/.env` with real strong values
- Configure persistence, backups, and access control for MySQL, Redis, RabbitMQ, and Elasticsearch
- Configure real domains, TLS, and CORS policy for the gateway and frontend
- Set resource limits and retention policies for logs, metrics, and tracing
- Adapt the Compose setup or move to Kubernetes, Helm, and CI/CD based on your target environment

## Troubleshooting

**`docker/.env` is missing or still contains placeholders**

- Copy `docker/.env.example` to `docker/.env`
- Replace all `change-me` and `replace-me` values

**`start-all.sh` fails on Java version checks**

- Make sure both `java -version` and `mvn -version` point to `Java 25`

**The frontend loads, but API requests fail**

- Check whether `gateway-service` or the relevant backend service is running
- Check whether the proxy targets in `web-app/vite.config.js` match your current run mode

**Services are up, but search does not work**

- Check the health of `search-service`, RabbitMQ, and Elasticsearch
- Confirm that course events have been published, or run the seed script first

## Documentation

- `docker/README.md`: Docker and local environment bootstrap
- `docs/README.md`: index of current operational docs
- `docs/gateway-service-guide.md`: gateway responsibilities and trust boundaries
- `docs/infrastructure-guide.md`: infrastructure and environment variable notes
- `docs/monitoring-guide.md`: monitoring and observability guidance

## License

This repository is licensed under the MIT License. See `LICENSE`.
