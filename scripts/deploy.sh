#!/bin/bash

set -e

DEPLOY_DIR="/home/ubuntu/cops-and-robbers"
cd "$DEPLOY_DIR"

echo "🚀 배포 스크립트 시작"

if [ -f .env ]; then
    echo "📦 기존 .env 파일 백업 중"
    cp .env .env.backup
fi

echo "📝 새 .env 파일 생성 중"
cat > .env << EOF
DB_PASSWORD=${DB_PASSWORD}
ACCESS_SECRET_KEY=${ACCESS_SECRET_KEY}
REFRESH_SECRET_KEY=${REFRESH_SECRET_KEY}
ACCESS_EXPIRATION=${ACCESS_EXPIRATION}
REFRESH_EXPIRATION=${REFRESH_EXPIRATION}
EOF
chmod 600 .env

echo "🐳 Docker Compose 배포 시작"
sudo docker compose -f docker-compose-prod.yml pull
sudo docker compose -f docker-compose-prod.yml down --remove-orphans
sudo docker compose -f docker-compose-prod.yml up -d

echo "🔍 Health Check 시작..."
MAX_RETRY=10
RETRY_INTERVAL=3

for i in $(seq 1 $MAX_RETRY); do
    echo "Health Check 시도 $i/$MAX_RETRY..."

    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 배포 성공! (${i}번째 시도에서 성공)"
        sudo docker image prune -f
        rm -f .env.backup
        exit 0
    fi

    if [ $i -lt $MAX_RETRY ]; then
        sleep $RETRY_INTERVAL
    fi
done

echo "❌ Health check 실패! 롤백 시작"
if [ -f .env.backup ]; then
    mv .env.backup .env
fi
