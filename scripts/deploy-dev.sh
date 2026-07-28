#!/bin/bash

set -e

DEPLOY_DIR="/home/ubuntu/cops-and-robbers"
DEV_ACTUATOR=9091

source "$(dirname "$0")/logging.sh"

cd "$DEPLOY_DIR"
init_logging

trap 'rm -f .env' EXIT
trap 'log "ERROR" "FAILED cmd=$BASH_COMMAND line=$LINENO"' ERR


# 1. .env 작성
cat > .env << EOF
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
ACCESS_SECRET_KEY=${ACCESS_SECRET_KEY}
REFRESH_SECRET_KEY=${REFRESH_SECRET_KEY}
ACCESS_EXPIRATION=${ACCESS_EXPIRATION}
REFRESH_EXPIRATION=${REFRESH_EXPIRATION}
ENCRYPTION_KEY=${ENCRYPTION_KEY}
DISCORD_WEBHOOK_BUG=${DISCORD_WEBHOOK_BUG}
EOF
chmod 600 .env
log "INFO" "ENV_CREATE_SUCCESS"


# 2. cops-network 상태 확인
# -> 수동 생성된 네트워크가 있으면 제거 후 compose가 재생성하도록 위임
NETWORK_LABEL=$(sudo docker network inspect cops-network --format '{{index .Labels "com.docker.compose.network"}}' 2>/dev/null || echo "NOT_FOUND")
if [ "$NETWORK_LABEL" = "" ]; then
    log "INFO" "NETWORK_RECREATE_START"
    sudo docker network rm cops-network
    log "INFO" "NETWORK_RECREATE_SUCCESS"
fi


# 3. Infra 상태 확인
# -> Redis 내려가 있을 때만 재가동
if ! sudo docker ps --format '{{.Names}}' | grep -q '^cops-and-robbers-redis$'; then
    log "INFO" "REDIS_NOT_RUNNING starting infra"
    sudo docker compose -f docker-compose-infra.yml up -d
    log "INFO" "INFRA_START_SUCCESS"
fi


# 4. 이미지 Pull 및 컨테이너 재시작
sudo docker compose -f docker-compose-dev-server.yml pull
log "INFO" "DOCKER_PULL_SUCCESS"

sudo docker compose -f docker-compose-dev-server.yml down
sudo docker compose -f docker-compose-dev-server.yml up -d
log "INFO" "CONTAINER_START_SUCCESS"


# 5. 헬스체크
MAX_RETRY=20
RETRY_INTERVAL=5

for i in $(seq 1 $MAX_RETRY); do
    if curl -sf "http://localhost:${DEV_ACTUATOR}/actuator/health" > /dev/null 2>&1; then
        log "INFO" "HEALTH_CHECK_SUCCESS attempt=$i"
        break
    fi

    if [ "$i" -eq "$MAX_RETRY" ]; then
        log "ERROR" "HEALTH_CHECK_FAILED"
        sudo docker compose -f docker-compose-dev-server.yml logs --tail 50 >> "$LOG_FILE" 2>&1 || true
        sudo docker compose -f docker-compose-dev-server.yml down
        rm -f .env
        finish_logging "ERROR"
        exit 1
    fi

    log "INFO" "HEALTH_CHECK_RETRY attempt=$i"
    sleep $RETRY_INTERVAL
done


# 6. 정리
sudo docker image prune -f
rm -f .env
finish_logging "INFO"
exit 0
