# Education Platform Infrastructure Guide

This guide explains how to start, inspect, and manage the infrastructure and application containers defined under `docker/`.

## Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Run Modes](#run-modes)
- [Services](#services)
- [Common Commands](#common-commands)
- [Access Points](#access-points)
- [Database Access](#database-access)
- [Troubleshooting](#troubleshooting)
- [Directory Layout](#directory-layout)

## Prerequisites

### 1. Install Docker

Install Docker Desktop on macOS or Windows, or Docker Engine with Compose on Linux.

Verify the installation:

```bash
docker --version
docker compose version
```

### 2. Understand the Java Baseline

- The pure Docker path, `./scripts/start-infra.sh`, does not require a host JDK. The Java services run inside containers on `Java 25`.
- The mixed local development path, `./scripts/start-all.sh`, runs `mvn spring-boot:run` on the host, so both `java` and `mvn` must target `Java 25`.

Check the host runtime if you plan to use the mixed local flow:

```bash
java -version
mvn -version
```

### 3. Make Sure Required Ports Are Free

The default setup uses these ports:

- `3306` for MySQL
- `6379` for Redis
- `8848`, `9848` for Nacos
- `5672`, `15672` for RabbitMQ
- `8080` for the gateway
- `9200` for Elasticsearch
- `9411` for Zipkin
- `3001` for Grafana

Example port check on macOS or Linux:

```bash
lsof -i :3306
```

If a required port is already in use, stop the conflicting process or change the port mapping in `docker/.env`.

## Quick Start

### 1. Enter the Project Directory

```bash
cd /path/to/edu_platform
```

### 2. Prepare Environment Variables

```bash
cp docker/.env.example docker/.env
```

Replace the placeholder values in `docker/.env`, especially:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `RABBITMQ_PASS`
- `JWT_SECRET`
- `INTERNAL_SERVICE_TOKEN`
- `GRAFANA_ADMIN_PASSWORD`

### 3. Start the Stack

```bash
./scripts/start-infra.sh --build --wait
```

On the first run, Docker may need several minutes to pull images and build application containers.

### 4. Verify the Result

```bash
docker ps
```

You should see the key services running, and the containers with health checks should move to `healthy`.

## Run Modes

### Option A: Docker Compose application stack

```bash
./scripts/start-infra.sh --build --wait
```

This starts the full Compose topology from `docker/docker-compose.yml`, including:

- Infrastructure services such as MySQL, Redis, Nacos, RabbitMQ, Elasticsearch, and Zipkin
- Application containers such as `gateway-service`, `user-service`, `course-service`, and `search-service`
- Observability services such as Prometheus, Grafana, Loki, and Promtail

Note that the frontend `web-app` is not containerized in the current Compose setup.

### Option B: Mixed local development

```bash
./scripts/start-all.sh
```

This script:

- Starts dependency containers with Docker
- Runs the backend services locally with Maven
- Runs the frontend locally with Vite
- Seeds demo data if the `e2e` dependencies are already installed

Use this mode when you want to debug application code locally.

## Services

| Service | Purpose | Port |
|---|---|---:|
| MySQL | Primary relational database | `3306` |
| Redis | Cache and session store | `6379` |
| Nacos | Service registry and configuration center | `8848`, `9848` |
| RabbitMQ | Message broker for async integration | `5672`, `15672` |
| Elasticsearch | Search engine | `9200` |
| Zipkin | Distributed tracing | `9411` |
| Gateway | Public API entry point | `8080` |
| Grafana | Metrics dashboards | `3001` |

## Common Commands

### Start Services

```bash
./scripts/start-infra.sh --build
./scripts/start-infra.sh --build --wait
```

### Stop Services

```bash
./scripts/stop-infra.sh
./scripts/stop-infra.sh --clean
```

`--clean` removes volumes and should be treated as destructive.

### View Logs

```bash
./scripts/logs.sh
./scripts/logs.sh mysql
./scripts/logs.sh nacos -f
```

### Restart a Single Container

```bash
cd docker
docker compose restart mysql
docker compose restart nacos
docker compose restart redis
```

## Access Points

### Nacos

- URL: `http://localhost:8848/nacos`
- Default username: `nacos`
- Default password: `nacos`

### RabbitMQ

- URL: `http://localhost:15672`
- Username: value of `RABBITMQ_USER` in `docker/.env`
- Password: value of `RABBITMQ_PASS` in `docker/.env`

### Zipkin

- URL: `http://localhost:9411`

### Elasticsearch

- URL: `http://localhost:9200`

### Grafana

- URL: `http://localhost:3001`
- Credentials: `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD` from `docker/.env`

## Database Access

### Connect to MySQL from the Command Line

```bash
docker exec -it edu-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD"
```

Common SQL commands:

```sql
SHOW DATABASES;
USE edu_user;
SHOW TABLES;
EXIT;
```

### Connect with a GUI Tool

Common tools include DBeaver, DataGrip, and Navicat.

Connection settings:

- Host: `localhost`
- Port: `3306`
- Username: `root`
- Password: `MYSQL_ROOT_PASSWORD` from `docker/.env`

Default databases created by the setup:

- `edu_user`
- `edu_course`

### Connect to Redis

```bash
docker exec -it edu-redis redis-cli
```

Useful Redis commands:

```text
PING
KEYS *
GET key_name
EXIT
```

## Troubleshooting

### Docker Is Not Running

Start Docker Desktop or the Docker daemon and retry.

### Port Already in Use

Identify the conflicting process and stop it, or adjust the port in `docker/.env`.

Example:

```bash
lsof -i :3306
kill -9 <PID>
```

### MySQL Keeps Restarting

Try a clean reset:

```bash
./scripts/stop-infra.sh --clean
./scripts/start-infra.sh --build --wait
```

### Image Downloads Are Slow

Configure a Docker registry mirror appropriate for your region.

### Apple Silicon Compatibility Issues

Some images do not natively support ARM. The current Compose setup already handles this for Nacos through x86 emulation. If you still see problems, verify that Docker Desktop has Rosetta support enabled for Apple Silicon.

### Services Start but Are Not Reachable

Check:

1. `docker ps` and container health
2. Host firewall or VPN interference
3. Whether `127.0.0.1` works better than `localhost`

## Directory Layout

```text
docker/
├── docker-compose.yml
├── .env
├── .env.example
├── mysql/
│   └── init/
│       └── 01-init-databases.sql
├── prometheus/
├── grafana/
├── loki/
└── promtail/
```

Related scripts:

```text
scripts/
├── start-infra.sh
├── stop-infra.sh
└── logs.sh
```

## Next Steps

After the infrastructure stack is running, you can:

1. Use `./scripts/start-all.sh` for mixed local development
2. Start the frontend with `cd web-app && npm install && npm run dev`
3. Inspect service registration in Nacos
4. Run E2E tests from `e2e/`
