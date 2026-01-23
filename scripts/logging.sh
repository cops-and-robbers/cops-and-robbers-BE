#!/bin/bash

LOG_DIR="/log/deploy"
LOG_RETENTION_DAYS=30
LOG_FILE=""

# 로깅 초기화
init_logging() {
    sudo mkdir -p "$LOG_DIR"
    local timestamp
    timestamp=$(date '+%Y%m%d_%H%M%S')
    LOG_FILE="$LOG_DIR/deploy_${timestamp}.log"

    log "INFO" "DEPLOY_START"
    cleanup_old_logs
}

# 로그 출력
log() {
    local level="$1"
    local event="$2"
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    echo "[${timestamp}] [${level}] ${event}" | sudo tee -a "$LOG_FILE"
}

# 배포 종료 로그
finish_logging() {
    local status="$1"
    log "$status" "DEPLOY_END"
}

# 오래된 로그 정리
cleanup_old_logs() {
    log "INFO" "CLEANUP_OLD_LOGS_START"
    sudo find "$LOG_DIR" -name "deploy_*.log" -mtime +"$LOG_RETENTION_DAYS" -delete 2>/dev/null || true
    log "INFO" "CLEANUP_OLD_LOGS_END"
}