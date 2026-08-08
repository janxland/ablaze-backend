# Ablaze Docker 部署

本服务适合使用 Docker。镜像采用 Maven 多阶段构建，运行层只保留 Java 8 JRE 和最终 fat jar；数据库不打包进镜像，继续复用宿主机已经运行的 MySQL、Redis 和 RabbitMQ。

## 首次部署

```bash
cp .env.docker.example .env.docker
chmod 600 .env.docker
# 编辑 .env.docker，填入真实凭据
install -d -m 750 -o 10001 -g 10001 logs
docker compose build
docker compose up -d
docker compose logs -f --tail=100 ablaze
```

Dockerfile 暴露 HTTP `8181` 和 T-IO WebSocket `9999`。这两个端口都要保留，因为 WebSocket 端口在 `ImConfigConst` 中是独立监听的。

## 宿主机数据库连接

容器使用 `host.docker.internal` 访问宿主机服务，Compose 已配置 `host-gateway`。默认连接为：

| 服务 | 容器内配置 | 说明 |
| --- | --- | --- |
| MySQL | `host.docker.internal:3306` | 数据库 `ablaze` |
| Redis | `host.docker.internal:26739` | 使用迁移记录中的宿主端口，不是旧配置中的 `6739` |
| RabbitMQ | `host.docker.internal:5672` | 复用宿主机 RabbitMQ |

如果这些服务只监听 `127.0.0.1`，Docker 的宿主网关可能无法连接。需要让服务监听宿主机 Docker 网桥可达的地址，并通过防火墙限制访问；不要把数据库端口直接暴露到公网。

## 更新与回滚

```bash
docker compose build
docker compose up -d --no-deps ablaze
docker compose ps
docker compose logs --tail=100 ablaze
```

For the Mac-to-server release path, use `./scripts/deploy-ablaze-docker.sh`. It builds
for `linux/amd64`, pushes an immutable tag plus `latest`, deploys with the server-only
`.env.docker`, verifies the service, and removes only this project's old image tags.

在切换前先确认宝塔中的旧 Java 进程已经停止，否则宿主机端口 `8181` 或 `9999` 会冲突。日志持久化在部署目录的 `logs/`，数据库数据不由本 Compose 管理。需要使用其他宿主机日志目录时，修改 Compose 中的 `./logs:/app/logs` 左侧路径。

## 资源建议

当前容器限制为 `1 CPU / 768 MB`，JVM 固定 `-Xms128m -Xmx512m`，Druid 连接池上限为 8。低流量服务器可以把 `DB_POOL_MAX_ACTIVE` 调到 5；不要在生产环境使用 `JPA_DDL_AUTO=update`，Docker profile 已固定为 `none`。

资源上传依赖七牛，AK/SK/桶名也只从环境变量读取。旧的用户密码加密常量暂时保留用于兼容存量数据，不要在未完成数据迁移前自行修改。

当前仓库历史配置曾包含数据库、邮箱、RabbitMQ、JWT 和七牛凭据。部署前应在对应平台轮换这些凭据，再写入 `.env.docker`；不要把该文件提交到 Git。
