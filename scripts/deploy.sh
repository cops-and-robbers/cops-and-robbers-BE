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

echo "⏳ Health check 대기 중 (30초)"
sleep 30

if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 배포 성공!"
    sudo docker image prune -f
    rm -f .env.backup
else
    echo "❌ Health check 실패! 롤백 시작"

    if [ -f .env.backup ]; then
        mv .env.backup .env
    fi
    sudo docker compose -f docker-compose-prod.yml up -d

    echo "⚠️ 롤백 완료. 서버 상태를 점검 바람"
    exit 1
fi
