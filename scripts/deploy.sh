#!/bin/bash

set -e

DEPLOY_DIR="/home/ubuntu/cops-and-robbers"

source "$(dirname "$0")/logging.sh"

cd "$DEPLOY_DIR"
init_logging

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

sudo docker compose -f docker-compose-prod.yml pull
log "INFO" "DOCKER_PULL_SUCCESS"

sudo docker compose -f docker-compose-prod.yml down --remove-orphans
log "INFO" "CONTAINER_STOP_SUCCESS"

sudo docker compose -f docker-compose-prod.yml up -d
log "INFO" "CONTAINER_START_SUCCESS"

MAX_RETRY=10
RETRY_INTERVAL=3

for i in $(seq 1 $MAX_RETRY); do
    if curl -f http://localhost:9091/actuator/health > /dev/null 2>&1; then
        log "INFO" "HEALTH_CHECK_SUCCESS attempt=$i"
        sudo docker image prune -f
        rm -f .env.backup
        finish_logging "INFO"
        exit 0
    fi

    if [ $i -lt $MAX_RETRY ]; then
        sleep $RETRY_INTERVAL
    fi
done

log "ERROR" "HEALTH_CHECK_FAILED"
if [ -f .env.backup ]; then
    mv .env.backup .env
    log "INFO" "ROLLBACK_SUCCESS"
fi
finish_logging "ERROR"
exit 1
