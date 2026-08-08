# Ablaze Docker 环境变量

此文件只记录变量名、用途和来源，不保存真实值。真实运行值保存在服务器：

```text
/www/wwwroot/ablaze-backend/docker/.env.docker
```

服务器文件权限必须是 `600`，不要将它复制进镜像、提交 Git 或写入 Dockerfile。

## 环境变量

| 变量 | 用途 | 当前来源 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring profile | 固定为 `docker` |
| `SERVER_PORT` | HTTP 端口 | `8181` |
| `DB_URL` | MySQL JDBC 地址 | 服务器 `.env.prod`，主机替换为 `host.docker.internal` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 凭据 | 服务器 `.env.prod` |
| `DB_POOL_MAX_ACTIVE` | Druid 最大连接数 | `8` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 | `host.docker.internal:26739` |
| `REDIS_USERNAME` / `REDIS_PASSWORD` | Redis 凭据 | 服务器 Redis `.env` |
| `REDIS_DATABASE` | Redis 数据库 | `0` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | RabbitMQ 地址 | `host.docker.internal:5672` |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 凭据 | 服务器 `.env.prod` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 凭据 | 服务器 `.env.prod` |
| `ALIPAY_APP_ID` | 支付宝应用 ID | 服务器 `.env.prod` |
| `ALIPAY_PRIVATE_KEY` / `ALIPAY_PUBLIC_KEY` | 支付宝密钥 | 服务器 `.env.prod` |
| `ALIPAY_NOTIFY_URL` | 支付回调地址 | 服务器 `.env.prod` |
| `PAYMENT_SECRET_KEY` / `PAYMENT_SIGN_FIELD` | 支付签名 | 服务器 `.env.prod` |
| `PAY_API_URL` / `PAY_STATUS_API_URL` | 支付接口 | 服务器 `.env.prod` |
| `JWT_SECRET` / `JWT_EXPIRATION` | JWT 配置 | 服务器 `.env.prod` |
| `LEGACY_CRYPTO_KEY` | 存量用户密码兼容加密密钥 | 旧版 `CommonConst`，不可随意修改 |
| `JWT_COMPAT_KEY` | 旧版 JWT 兼容密钥 | 旧版 `CommonConst` |
| `QINIU_ACCESS_KEY` / `QINIU_SECRET_KEY` | 七牛凭据 | 旧版 `CommonConst` |
| `QINIU_BUCKET` | 七牛 Bucket | 旧版 `CommonConst` |

## 一键发布

先在本机完成一次 Docker Hub 登录：

```bash
docker login
```

之后在本目录执行：

```bash
./scripts/deploy-ablaze-docker.sh
```

脚本默认使用 `BUILD_PLATFORM=linux/amd64`，适配当前服务器；如目标服务器架构不同，可在执行时覆盖该变量。

链路为：本地仓库 -> Docker Hub `roginx/ablaze-backend` -> 服务器拉取不可变版本标签 -> 重建 `ablaze-0` -> HTTP/WebSocket/API 验证 -> 删除旧 `ablaze:local` 镜像。
