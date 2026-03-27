# 在线教育平台 - 基础设施使用指南

本指南帮助你启动和管理教育平台所需的基础设施服务。

## 目录

- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [服务说明](#服务说明)
- [常用命令](#常用命令)
- [访问管理界面](#访问管理界面)
- [常见问题](#常见问题)

---

## 前置要求

### 1. 安装 Docker Desktop

**Mac 用户：**
1. 访问 https://www.docker.com/products/docker-desktop/
2. 下载 Mac 版本（注意选择 Apple Silicon 或 Intel 芯片版本）
3. 双击下载的 `.dmg` 文件，将 Docker 拖入 Applications
4. 打开 Docker Desktop，等待状态变为 "Running"

**Windows 用户：**
1. 访问 https://www.docker.com/products/docker-desktop/
2. 下载 Windows 版本
3. 运行安装程序，按提示完成安装
4. 可能需要重启电脑
5. 打开 Docker Desktop，等待状态变为 "Running"

**验证安装：**
打开终端（Mac）或 PowerShell（Windows），运行：
```bash
docker --version
```
如果显示版本号（如 `Docker version 24.0.0`），说明安装成功。

### 2. 了解 Java 运行基线

- 纯 Docker 启动路径 `./scripts/start-infra.sh` 不依赖宿主机 JDK，Java 服务会在容器内以 `Java 25` 运行。
- 本地混合开发路径 `./scripts/start-all.sh` 会直接执行本机 `mvn spring-boot:run`，因此要求宿主机上的 `java` 和 `mvn` 都指向 `Java 25`。

可用下面的命令确认本机 JDK：

```bash
java -version
mvn -version
```

### 3. 确保端口未被占用

以下端口需要可用：
- 3306 (MySQL)
- 6379 (Redis)
- 8848, 9848 (Nacos)
- 5672, 15672 (RabbitMQ)
- 9200 (Elasticsearch)
- 9411 (Zipkin)

**Mac 检查端口占用：**
```bash
lsof -i :3306
```

**如果端口被占用，需要先停止占用该端口的程序。**

---

## 快速开始

### 第一步：打开终端

**Mac：**
- 按 `Command + 空格`，输入 "Terminal"，回车

**Windows：**
- 按 `Win + R`，输入 "powershell"，回车

### 第二步：进入项目目录

```bash
cd /path/to/edu_platform
```

将 `/path/to/edu_platform` 替换为你的实际项目路径。

### 第三步：准备环境变量

```bash
cp docker/.env.example docker/.env
```

按需修改 `docker/.env` 中的密码和端口。

### 第四步：启动所有服务

```bash
./scripts/start-infra.sh --build
```

首次运行会下载镜像，可能需要 5-15 分钟（取决于网络速度）。

**看到以下输出说明启动成功：**
```
==========================================
  Services Started!
==========================================

Access URLs:
  • MySQL:         localhost:3306
  • Redis:         localhost:6379
  • Nacos:         http://localhost:8848/nacos
  • RabbitMQ:      http://localhost:15672
  • Elasticsearch: http://localhost:9200
  • Zipkin:        http://localhost:9411
```

### 第五步：验证服务状态

```bash
docker ps
```

应该看到 6 个容器都显示 `(healthy)` 状态。

### 第六步：进入本地混合开发模式（可选）

如果你要在本机调试后端服务，而不是使用 Compose 里的应用容器：

```bash
./scripts/start-all.sh
```

这个脚本会先启动 Docker 依赖，再在宿主机上运行各个 Maven 服务和前端。它会在启动前检查本机 `java` 是否为 `Java 25`，不满足时直接退出。

---

## 服务说明

| 服务 | 用途 | 端口 |
|------|------|------|
| **MySQL** | 主数据库，存储用户、课程、订单等数据 | 3306 |
| **Redis** | 缓存服务，存储登录会话、热点数据 | 6379 |
| **Nacos** | 服务注册中心 + 配置中心 | 8848 |
| **RabbitMQ** | 消息队列，处理异步任务（订单通知等） | 5672, 15672 |
| **Elasticsearch** | 搜索引擎，支持课程全文搜索 | 9200 |
| **Zipkin** | 链路追踪，监控服务调用 | 9411 |

---

## 常用命令

### 启动服务

```bash
# 启动所有服务
./scripts/start-infra.sh --build

# 启动并等待所有服务健康（推荐）
./scripts/start-infra.sh --build --wait
```

### 停止服务

```bash
# 停止所有服务（保留数据）
./scripts/stop-infra.sh

# 停止并删除所有数据（慎用！）
./scripts/stop-infra.sh --clean
```

### 查看日志

```bash
# 查看所有服务日志
./scripts/logs.sh

# 查看指定服务日志
./scripts/logs.sh mysql
./scripts/logs.sh nacos
./scripts/logs.sh redis

# 实时跟踪日志（按 Ctrl+C 退出）
./scripts/logs.sh mysql -f
```

### 查看服务状态

```bash
docker ps
```

### 重启单个服务

```bash
cd docker
docker compose restart mysql    # 重启 MySQL
docker compose restart nacos    # 重启 Nacos
docker compose restart redis    # 重启 Redis
```

---

## 访问管理界面

### Nacos 控制台（服务注册 & 配置中心）

- **地址**：http://localhost:8848/nacos
- **用户名**：`nacos`
- **密码**：`nacos`

![Nacos 界面说明]
- 左侧菜单「服务管理」→「服务列表」：查看已注册的微服务
- 左侧菜单「配置管理」→「配置列表」：管理各服务配置

### RabbitMQ 控制台（消息队列）

- **地址**：http://localhost:15672
- **用户名**：查看 `docker/.env` 中的 `RABBITMQ_USER`
- **密码**：查看 `docker/.env` 中的 `RABBITMQ_PASS`

![RabbitMQ 界面说明]
- Queues 标签：查看消息队列
- Connections 标签：查看连接状态

### Zipkin 控制台（链路追踪）

- **地址**：http://localhost:9411
- **无需登录**

用于追踪请求在各微服务之间的调用链路，排查性能问题。

### Elasticsearch

- **地址**：http://localhost:9200
- **无需登录**

直接访问会显示集群信息，一般通过 API 或 Kibana 使用。

---

## 数据库连接

### 使用命令行连接 MySQL

```bash
docker exec -it edu-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD"
```

常用 SQL 命令：
```sql
-- 查看所有数据库
SHOW DATABASES;

-- 切换到用户数据库
USE edu_user;

-- 查看所有表
SHOW TABLES;

-- 退出
EXIT;
```

### 使用图形化工具连接

推荐工具：[DBeaver](https://dbeaver.io/)（免费）、Navicat、DataGrip

连接参数：
- **主机**：`localhost`
- **端口**：`3306`
- **用户名**：`root`
- **密码**：查看 `docker/.env` 中的 `MYSQL_ROOT_PASSWORD`

已创建的数据库：
- `edu_user` - 用户服务数据库
- `edu_course` - 课程服务数据库

### 连接 Redis

```bash
docker exec -it edu-redis redis-cli
```

常用命令：
```bash
PING          # 测试连接，返回 PONG
KEYS *        # 查看所有 key
GET key_name  # 获取指定 key 的值
EXIT          # 退出
```

---

## 常见问题

### Q1: 启动时提示 "Docker is not running"

**解决方法**：打开 Docker Desktop 应用，等待状态变为 Running。

### Q2: 启动时提示端口被占用 (address already in use)

**解决方法**：

**Mac - 查找并停止占用端口的进程：**
```bash
# 查看占用 3306 端口的进程
lsof -i :3306

# 停止进程（将 PID 替换为实际进程ID）
kill PID
```

**或者修改端口**：编辑 `docker/.env` 文件，修改对应端口号。

### Q3: MySQL 一直显示 "Restarting"

**解决方法**：清除数据卷后重新启动
```bash
cd docker
docker compose stop mysql
docker compose rm -f mysql
docker volume rm docker_mysql-data
docker compose up -d mysql
```

### Q4: 镜像下载很慢

**解决方法**：配置 Docker 镜像加速器

1. 打开 Docker Desktop
2. 点击设置（齿轮图标）
3. 选择 Docker Engine
4. 在 JSON 配置中添加：
```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
```
5. 点击 "Apply & Restart"

### Q5: Mac M1/M2/M3 芯片启动失败

部分镜像不支持 ARM 架构，本配置已处理此问题（Nacos 使用 x86 模拟运行）。

如仍有问题，确保 Docker Desktop 已启用 Rosetta：
1. 打开 Docker Desktop 设置
2. 勾选 "Use Rosetta for x86/amd64 emulation on Apple Silicon"

### Q6: 如何完全重置所有数据？

```bash
./scripts/stop-infra.sh --clean
./scripts/start-infra.sh
```

⚠️ **警告**：此操作会删除所有数据，包括数据库内容！

### Q7: 服务启动后无法访问

1. 检查服务状态：`docker ps`，确认都是 healthy
2. 检查防火墙是否阻止了端口访问
3. 尝试使用 `127.0.0.1` 替代 `localhost`

---

## 目录结构说明

```
docker/
├── docker-compose.yml      # Docker 服务配置文件
├── .env                    # 环境变量（密码等）
├── .env.example            # 环境变量示例
└── mysql/
    └── init/
        └── 01-init-databases.sql  # 数据库初始化脚本

scripts/
├── start-infra.sh          # 启动脚本
├── stop-infra.sh           # 停止脚本
└── logs.sh                 # 日志查看脚本
```

---

## 获取帮助

如果遇到问题：

1. 查看服务日志：`./scripts/logs.sh 服务名`
2. 检查 Docker Desktop 是否正常运行
3. 尝试重启服务：`docker compose restart 服务名`
4. 查看本文档的「常见问题」部分

---

## 下一步

基础设施启动成功后，可以：

1. 使用 `./scripts/start-all.sh` 进入本地混合开发模式
2. 启动前端开发服务器
3. 在 Nacos 控制台查看服务注册情况
