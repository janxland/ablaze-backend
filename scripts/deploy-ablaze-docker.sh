#!/usr/bin/env bash
set -Eeuo pipefail

readonly IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-roginx/ablaze-backend}"
readonly REMOTE_HOST="${REMOTE_HOST:-root@c202608060983447.tail581631.ts.net}"
readonly REMOTE_DIR="${REMOTE_DIR:-/www/wwwroot/ablaze-backend/docker}"
readonly BUILD_PLATFORM="${BUILD_PLATFORM:-linux/amd64}"
readonly VERSION="${IMAGE_TAG:-$(date -u +%Y%m%d-%H%M%S)-$(git rev-parse --short HEAD)}"
readonly IMAGE="${IMAGE_REPOSITORY}:${VERSION}"
readonly LATEST_IMAGE="${IMAGE_REPOSITORY}:latest"

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

command -v docker >/dev/null || fail "docker is required"
command -v tailscale >/dev/null || fail "tailscale is required"
test -f Dockerfile || fail "run this script from the backend repository root"
test -f .env.docker.example || fail ".env.docker.example is missing"
test -f docker-compose.yml || fail "docker-compose.yml is missing"

if rg -n -i 'spring\.(datasource|redis|rabbitmq)\.(url|password|host)=[^$]|alipay\.(appPrivateKey|alipayPublicKey)=[^$]|jwt\.secret=[^$]|(ACCESS_KEY|SECRET_KEY)\s*=\s*"[^$]' src -g '*.java' -g '*.properties' >/tmp/ablaze-sensitive-source-scan; then
    sed -n '1,40p' /tmp/ablaze-sensitive-source-scan >&2
    rm -f /tmp/ablaze-sensitive-source-scan
    fail "sensitive literal detected in the Docker build context"
fi
rm -f /tmp/ablaze-sensitive-source-scan

printf 'Building %s\n' "$IMAGE"
# Reuse cached base images when available; Docker pulls missing bases automatically.
docker build --platform "$BUILD_PLATFORM" --tag "$IMAGE" --tag "$LATEST_IMAGE" .

printf 'Pushing %s and %s\n' "$IMAGE" "$LATEST_IMAGE"
docker push "$IMAGE"
docker push "$LATEST_IMAGE"

printf 'Syncing deployment metadata to %s:%s\n' "$REMOTE_HOST" "$REMOTE_DIR"
COPYFILE_DISABLE=1 tar -czf - docker-compose.yml DOCKER.md | \
    tailscale ssh "$REMOTE_HOST" "install -d -m 755 '$REMOTE_DIR'; tar -xzf - -C '$REMOTE_DIR'"

printf 'Pulling and starting %s on the server\n' "$IMAGE"
tailscale ssh "$REMOTE_HOST" "set -Eeuo pipefail
test -f '$REMOTE_DIR/.env.docker'
test \"\$(stat -c '%a' '$REMOTE_DIR/.env.docker')\" = 600
cd '$REMOTE_DIR'
ABLAZE_IMAGE='$IMAGE' docker compose pull ablaze
ABLAZE_IMAGE='$IMAGE' docker compose up -d --no-build --force-recreate ablaze
for attempt in \$(seq 1 18); do
    if curl -fsS --max-time 3 http://127.0.0.1:8181/webInfo/getWebInfo >/dev/null; then
        break
    fi
    test \"\$attempt\" -lt 18 || { docker compose logs --tail=160 ablaze; exit 1; }
    sleep 5
done
test \"\$(docker inspect -f '{{.State.Running}}' ablaze-0)\" = true
nc -z 127.0.0.1 8181
nc -z 127.0.0.1 9999
echo 'Deployment verified'
docker compose ps
docker image inspect '$IMAGE' --format 'server_image={{.Id}} size={{.Size}}'
if docker image inspect ablaze:local >/dev/null 2>&1; then
    docker image rm ablaze:local >/dev/null || true
fi
for old_image in \$(docker image ls '$IMAGE_REPOSITORY' --format '{{.Repository}}:{{.Tag}}' | grep -v -F '$IMAGE' || true); do
    docker image rm "\$old_image" >/dev/null || true
done"

for local_image in $(docker image ls "$IMAGE_REPOSITORY" --format '{{.Repository}}:{{.Tag}}' || true); do
    docker image rm "$local_image" >/dev/null 2>&1 || true
done
printf 'Deployment complete: %s\n' "$IMAGE"
