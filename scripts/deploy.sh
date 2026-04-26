#!/bin/bash

set -e

DEPLOY_DIR="/home/ubuntu/cops-and-robbers"

NGINX_DIR="/etc/nginx/sites-available"
UPSTREAM_BLUE="$NGINX_DIR/upstream.blue.conf"
UPSTREAM_GREEN="$NGINX_DIR/upstream.green.conf"
UPSTREAM_ACTIVE="$NGINX_DIR/upstream.active.conf"

BLUE_PORT=8080
BLUE_ACTUATOR=9091
GREEN_PORT=8081
GREEN_ACTUATOR=9092

source "$(dirname "$0")/logging.sh"

cd "$DEPLOY_DIR"
init_logging

trap 'log "ERROR" "FAILED cmd=$BASH_COMMAND line=$LINENO"' ERR


# 1. .env 작성
# -> CI 환경변수로 재생성. 보안상 배포 완료 후 8번에서 삭제
if [ -f .env ]; then
    cp .env .env.backup
    log "INFO" "ENV_BACKUP_SUCCESS"
fi

cat > .env << EOF
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
ACCESS_SECRET_KEY=${ACCESS_SECRET_KEY}
REFRESH_SECRET_KEY=${REFRESH_SECRET_KEY}
ACCESS_EXPIRATION=${ACCESS_EXPIRATION}
REFRESH_EXPIRATION=${REFRESH_EXPIRATION}
ENCRYPTION_KEY=${ENCRYPTION_KEY}
EOF
chmod 600 .env
log "INFO" "ENV_CREATE_SUCCESS"


# 2. Infra 상태 확인
# ->  Redis 내려가 있을 때만 재가동
if ! sudo docker ps --format '{{.Names}}' | grep -q '^cops-and-robbers-redis$'; then
    log "INFO" "REDIS_NOT_RUNNING starting infra"
    sudo docker compose -f docker-compose-infra.yml up -d
    log "INFO" "INFRA_START_SUCCESS"
fi


# 3. Active 감지
# -> 현재 가동중인 색상 판단 및 변수 세팅 (target: 새로 배포할 색상 / current: 기존 배포 색상)
ACTIVE_LINK=$(readlink "$UPSTREAM_ACTIVE" 2>/dev/null || echo "")

if [ "$ACTIVE_LINK" = "$UPSTREAM_BLUE" ]; then
    CURRENT=blue
    TARGET=green
    TARGET_PORT=$GREEN_PORT
    TARGET_ACTUATOR=$GREEN_ACTUATOR
elif [ "$ACTIVE_LINK" = "$UPSTREAM_GREEN" ]; then
    CURRENT=green
    TARGET=blue
    TARGET_PORT=$BLUE_PORT
    TARGET_ACTUATOR=$BLUE_ACTUATOR
else
    TARGET_PORT=$BLUE_PORT
    TARGET_ACTUATOR=$BLUE_ACTUATOR
    TARGET=blue

    if sudo docker ps --format '{{.Names}}' | grep -q '^cops-and-robbers-app$'; then
        CURRENT=legacy
        TARGET=green
        TARGET_PORT=$GREEN_PORT
        TARGET_ACTUATOR=$GREEN_ACTUATOR
    else
        CURRENT=none
    fi
fi

log "INFO" "DEPLOY_START current=$CURRENT target=$TARGET"


# 4. Target 컨테이너 시작
# -> target 포트를 점유한 잔여 컨테이너가 있다면 정리 후 기동 (현재 실행중인 current는 내리지 않음)
sudo docker compose -f "docker-compose-${TARGET}.yml" pull
log "INFO" "DOCKER_PULL_SUCCESS target=$TARGET"

sudo docker compose -f "docker-compose-${TARGET}.yml" stop  2>/dev/null || true
sudo docker compose -f "docker-compose-${TARGET}.yml" rm -f 2>/dev/null || true
sudo docker compose -f "docker-compose-${TARGET}.yml" up -d
log "INFO" "TARGET_START_SUCCESS target=$TARGET"


# 5. 헬스체크
# -> Actuator health로 target 가동 확인
MAX_RETRY=20
RETRY_INTERVAL=5

for i in $(seq 1 $MAX_RETRY); do
    if curl -sf "http://localhost:${TARGET_ACTUATOR}/actuator/health" > /dev/null 2>&1; then
        log "INFO" "HEALTH_CHECK_SUCCESS target=$TARGET attempt=$i"
        break
    fi

    if [ "$i" -eq "$MAX_RETRY" ]; then
        log "ERROR" "HEALTH_CHECK_FAILED target=$TARGET"
        sudo docker compose -f "docker-compose-${TARGET}.yml" logs --tail 50 >> "$LOG_FILE" 2>&1 || true
        sudo docker compose -f "docker-compose-${TARGET}.yml" down
        finish_logging "ERROR"
        exit 1
    fi

    log "INFO" "HEALTH_CHECK_RETRY attempt=$i target=$TARGET"
    sleep $RETRY_INTERVAL
done


# 6. Nginx 전환
# -> 심볼릭 링크를 target으로 교체 후 reload
sudo ln -sfn "$NGINX_DIR/upstream.${TARGET}.conf" "$UPSTREAM_ACTIVE"

if ! sudo nginx -t > /dev/null 2>&1; then
    log "ERROR" "NGINX_CONFIG_INVALID"
    [ "$CURRENT" != "none" ] && [ "$CURRENT" != "legacy" ] && \
        sudo ln -sfn "$NGINX_DIR/upstream.${CURRENT}.conf" "$UPSTREAM_ACTIVE"
    sudo docker compose -f "docker-compose-${TARGET}.yml" down
    finish_logging "ERROR"
    exit 1
fi

sudo nginx -s reload
log "INFO" "NGINX_RELOAD_SUCCESS target=$TARGET port=$TARGET_PORT"


# 7. Active Graceful 종료
# -> SIGTERM 후 40초 대기 (Spring shutdown 30s + 여유 10s)
if [ "$CURRENT" = "blue" ] || [ "$CURRENT" = "green" ]; then
    sudo docker compose -f "docker-compose-${CURRENT}.yml" stop -t 40 || true
    sudo docker compose -f "docker-compose-${CURRENT}.yml" down || true
    log "INFO" "CURRENT_STOP_SUCCESS current=$CURRENT"
elif [ "$CURRENT" = "legacy" ]; then
    sudo docker stop -t 40 cops-and-robbers-app 2>/dev/null || true
    sudo docker rm cops-and-robbers-app 2>/dev/null || true
    log "INFO" "LEGACY_STOP_SUCCESS"
fi


# 8. 불필요한 것들 정리
sudo docker image prune -f
rm -f .env .env.backup
finish_logging "INFO"
exit 0
