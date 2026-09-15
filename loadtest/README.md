# 부하테스트 실행 가이드

계획 문서: [`docs/load-test-plan.md`](../docs/load-test-plan.md)

## 구성

| 파일 | 역할 |
|---|---|
| `lib/stomp.js` | STOMP 1.2 프레임 조립·파싱, 구독 채널 목록 |
| `profile-a-join.js` | 프로파일 A — 입장 폭주 (DB 바운드) |
| `profile-b-play.js` | 프로파일 B — 게임 진행 (팬아웃 바운드) |
| `players.json` | 시딩으로 생성되는 토큰 파일. Git에 올리지 않는다 |

서버 쪽 시딩은 `src/main/java/com/team/cops_and_robbers/loadtest/` 에 있다.

---

## 1. 데이터 시딩

170명(A방 20 / B방 100 / C방 50)과 게임 3개를 만들고, 각 게임을 진행 상태까지 올린 뒤
k6가 읽을 `players.json`을 생성한다.

> **프로파일 주의**: 이 프로젝트에는 `dev`와 `prod` 두 프로파일뿐이고 `local`이 없다.
> 로컬과 OCI 개발서버가 **같은 `dev` 프로파일**을 쓰며, 구분은 env 변수(`DB_URL`, `REDIS_HOST`)로만 한다.
> 따라서 시딩은 두 곳 모두에서 동작한다.

### 로컬

`logback-spring.xml`의 `LOG_PATH`가 컨테이너 경로 `/log`로 고정이라, 로컬에서 그냥 부팅하면
logback 초기화에서 `FileNotFoundException`으로 죽는다. 그래서 `--logging.config`로
콘솔 전용 설정(`loadtest/console-only-logback.xml`, 레포에 포함)을 지정해야 한다.

```bash
# 로컬 인프라 (postgres-main 5432, redis-main 6379)
docker compose -f docker-compose-dev.yml up -d postgres-main redis-main

# 시딩 플래그를 켜고 부팅
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun \
  --args='--logging.config=./loadtest/console-only-logback.xml --loadtest.seed.enabled=true --loadtest.seed.output=./loadtest/players.json'
```

### OCI 개발서버 (실행 단계 2)

서버가 이미 `SPRING_PROFILES_ACTIVE=dev`로 돌고 있으므로 플래그만 주면 된다.
**`docker-compose-dev-server.yml`은 git에 추적되는 파일이라 직접 수정하지 않는다.**
실수로 커밋되면 개발서버가 부팅마다 170명을 만들게 된다.

일회성 컨테이너로 주입한다.

```bash
docker compose -f docker-compose-dev-server.yml run --rm \
  -e LOADTEST_SEED_ENABLED=true \
  -e LOADTEST_SEED_OUTPUT=/log/players.json \
  cops-and-robbers-dev
```

`run --rm`은 포트를 매핑하지 않아 실행 중인 서버와 충돌하지 않고, 끝나면 컨테이너가 사라진다.
`/log`는 호스트의 `/home/ubuntu/log`에 마운트되어 있으므로 생성된 `players.json`을
`scp`로 내려받아 부하 발생기(로컬 맥)에서 쓴다.

- `loadtest.seed.enabled=true` 일 때만 동작한다. 평소 dev 부팅에는 영향이 없다.
- 이미 시딩되어 있으면(`LTA001` 초대코드 존재) 건너뛴다. 다시 만들려면 DB를 비운다.
- 유저 `socialId`에 `loadtest-` 접두사가 붙으므로 정리할 때 이 값으로 찾으면 된다.

### 시딩이 하는 일

1. 방 3개 생성 (`LTA001` / `LTB001` / `LTC001`), `maxParticipants`를 방 크기로 설정
2. 인원을 경찰/도둑 **1:1**로 번갈아 배정, index 0을 방장으로
3. `JwtTokenProvider`로 access token 발급 — 기존 `DataLoader`와 동일한 경로라 별도 JWT 작업이 없다
4. 게임 시작: `startGame` → `openGameResult` → `scheduleAllEvents` → `loadCache`

> 4번의 `loadCache`가 핵심이다. 위치·채팅 publish가 `InGameParticipantCache`(Redis)를 요구하므로,
> 이걸 빼먹으면 모든 publish가 `PARTICIPANT_NOT_FOUND`로 실패한다.

---

## 2. 프로파일 A — 입장 폭주

```bash
k6 run -e WS_URL=ws://localhost:8080/connection loadtest/profile-a-join.js
```

170명을 60초에 투입해 접속 → 7채널 구독 → 도둑은 위치 1회 발사까지 재현한다.

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `WS_URL` | `ws://localhost:8080/connection` | STOMP 엔드포인트 |
| `PLAYERS` | `./players.json` | 토큰 파일 경로 |
| `RAMP_SECONDS` | `60s` | 램프업 시간 |
| `HOLD_SECONDS` | `60` | 투입 후 유지 시간 |

**측정 지표**

- `stomp_connect_duration` — CONNECT 왕복. 서버의 `existsById` 1회 포함
- `stomp_subscribe_duration` — CONNECTED부터 자기 채팅이 되돌아올 때까지. 구독 7건이 실제로 붙었음을 증명
- `join_success` — 합격선 99%
- `stomp_error_frames` — 합격선 0

> 구독 완료를 RECEIPT가 아니라 **자기 메시지 echo**로 판정한다.
> Spring SimpleBroker가 RECEIPT 프레임을 보장하지 않기 때문이다.

---

## 3. 프로파일 B — 게임 진행

```bash
k6 run -e WS_URL=ws://localhost:8080/connection loadtest/profile-b-play.js
```

3방을 동시에 15분 굴린다. B방(100명) 채팅을 전체 구간 3등분해 **1 → 5 → 10 msg/s**로 올리며,
팬아웃이 방 크기에 비례하므로 outbound가 약 100 → 500 → 1,000 send/s로 커진다.

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `DURATION_MINUTES` | `15` | 유지 시간 |
| `LOCATION_INTERVAL_MS` | `5000` | 도둑 위치 전송 주기. FE 정책상 최대 5초에 1건 |
| `PING_INTERVAL_MS` | `20000` | 경찰 핑 주기 |

**측정 지표**

- `stomp_chat_rtt{room:A|B|C}` — **방 크기별로 분리**. 20명 방은 멀쩡한데 100명 방만 느려지면 팬아웃이 원인으로 특정된다
- `stomp_chat_sent` / `stomp_messages_received` — 팬아웃 실측.
  수신 쪽은 채팅뿐 아니라 위치공개·핑·시스템 이벤트까지 **모든 MESSAGE 프레임**을 센다 (= outbound 총량)
- `stomp_location_sent` — 도둑 위치 전송량. 계획 §5의 상한 17 msg/s와 대조해 모델이 맞는지 확인
- `session_alive` — 중도 끊김 탐지

---

## 4. 측정값 수집

**모니터링 서버는 부하테스트 이후에 붙이기로 했으므로, 지금은 직접 저장해야 한다.**
k6는 기본적으로 stdout에만 요약을 찍고 끝나서, 저장하지 않으면 측정값이 남지 않는다.

### 4-1. 클라이언트 쪽 (k6)

```bash
mkdir -p loadtest/out

k6 run \
  --out json=loadtest/out/profile-b.json.gz \
  --summary-export=loadtest/out/profile-b-summary.json \
  -e WS_URL=ws://서버:8080/connection \
  loadtest/profile-b-play.js
```

- `--summary-export` — 집계값만. 합격 기준 판정에는 이것으로 충분하다
- `--out json=` — 원시 샘플 전부. 시간축으로 그려보려면 필요하다
  - **용량 주의**: 파일명이 `.gz`로 끝나면 k6가 자동 압축한다.
    170 VU 실측 기준 1분에 비압축 6.3 MB / 압축 0.2 MB이므로,
    15분이면 **비압축 약 95 MB, 압축 약 3 MB**다. `.gz`를 꼭 붙인다

### 4-2. 서버 쪽 (Prometheus 스크랩)

테스트 시작할 때 같이 띄워두고, 끝나면 `Ctrl+C`로 멈춘다.

```bash
mkdir -p loadtest/out

while sleep 5; do
  echo "### $(date -Iseconds)" >> loadtest/out/server-metrics.txt
  curl -s http://서버:9091/actuator/prometheus >> loadtest/out/server-metrics.txt
done
```

액추에이터는 `9091` 포트에 열려 있다(`docker-compose-dev-server.yml`).

### 4-3. 무엇을 보나

| 대상 | 지표 | 왜 |
|---|---|---|
| HikariCP | `hikaricp_connections_pending` | 프로파일 A의 1순위 병목 |
| JVM | `jvm_memory_used_bytes`, `jvm_gc_pause_seconds` | 팬아웃 적체가 힙으로 나타난다 |
| FCM | `executor_queued_tasks{name="fcmExecutor"}` | `DiscardOldestPolicy`라 적체 시 푸시가 조용히 버려진다 |
| WebSocket 팬아웃 | `executor_queued_tasks{name="clientOutboundChannelExecutor"}` | **프로파일 B의 1순위 병목.** 계속 쌓이면 팬아웃이 밀리는 중이고 곧 힙 적체로 이어진다 |
| WebSocket 인바운드 | `executor_queued_tasks{name="clientInboundChannelExecutor"}` | 입장 폭주 때 SUBSCRIBE 처리가 밀리는지 |
| WebSocket 세션 수 | 미노출 | **아래 4-4 참고** |
| EC2 | CPU%, `CPUCreditBalance` | 운영 검증 시에만 (CloudWatch) |

### 4-4. 세션 수만 로그로 본다

채널 큐(`clientInboundChannelExecutor` / `clientOutboundChannelExecutor`)는
**프로메테우스에 이미 `executor_queued_tasks`로 나온다.** Spring이 이 익스큐터들을
`ThreadPoolTaskExecutor` 타입으로 선언해 두어서 액추에이터가 자동으로 잡는다.
1순위 병목 지표는 4-2의 스크랩만으로 시계열로 남는다.

노출되지 않는 건 **WebSocket 세션 수**뿐이다(`tomcat_sessions_*`는 HTTP 세션이라 무관).
Gauge를 새로 등록하는 건 범위가 커서, 이미 있는 `WebSocketMessageBrokerStats`의
로깅 주기만 낮춰 로그로 본다. 기본 30분이라 그대로는 못 쓴다.

```bash
--loadtest.stats.logging-period-ms=10000
```

`LoadTestBrokerStatsLogger`가 이 프로퍼티가 있을 때만 등록되므로 **플래그를 빼면 원래대로 돌아간다.**
코드를 되돌릴 필요가 없다.

10초마다 이런 줄이 찍힌다.

```
WebSocketSession[170 current WS(170)-HttpStream(0)-HttpPoll(0), 170 total, 0 closed abnormally]
clientInboundChannel[pool size = 4, active threads = 0, queued tasks = 0, completed tasks = 8213]
clientOutboundChannel[pool size = 4, active threads = 0, queued tasks = 0, completed tasks = 84870]
```

`closed abnormally` 수가 중도 이탈과 맞물리는지 보면 프로파일 B의 `session_alive`와 교차검증이 된다.

---

## 5. 정리

시딩 데이터는 `loadtest-` 접두사로 찾을 수 있다. OCI 개발서버에서 테스트한 뒤에는 반드시 정리한다.

```sql
-- FK 역순으로 지운다. 테이블명은 엔티티 @Table 매핑에서 확인한 값이다.
BEGIN;

CREATE TEMP TABLE lt_games AS
  SELECT id FROM games WHERE invite_code IN ('LTA001','LTB001','LTC001');
CREATE TEMP TABLE lt_users AS
  SELECT id FROM users WHERE social_id LIKE 'loadtest-%';

DELETE FROM game_result_participants
  WHERE game_result_id IN (SELECT id FROM game_results WHERE game_id IN (SELECT id FROM lt_games));
DELETE FROM reports          WHERE game_id IN (SELECT id FROM lt_games)
                                OR reporter_user_id IN (SELECT id FROM lt_users)
                                OR reported_user_id IN (SELECT id FROM lt_users);
DELETE FROM game_results     WHERE game_id IN (SELECT id FROM lt_games);
DELETE FROM participants     WHERE game_id IN (SELECT id FROM lt_games);
DELETE FROM game_areas       WHERE game_id IN (SELECT id FROM lt_games);
DELETE FROM games            WHERE id      IN (SELECT id FROM lt_games);
DELETE FROM user_devices     WHERE user_id IN (SELECT id FROM lt_users);
DELETE FROM users            WHERE id      IN (SELECT id FROM lt_users);

COMMIT;
```

> `reports`는 `game_id` / `reporter_user_id` / `reported_user_id` 에 FK가 없어 삭제를 막지는 않지만,
> 그냥 두면 고아 행이 남는다. 부하 시나리오가 신고를 만들지는 않으므로 보통 0건이지만 같이 지운다.
>
> `games`를 참조하는 테이블이 더 있으면 FK 제약으로 막힌다.
> 그때는 에러 메시지에 나온 테이블을 `participants` 줄 위에 추가하면 된다.

Redis 잔여 키도 지워야 하는데, **개발서버 Redis는 다른 게임·다른 개발자와 공유된다.**
와일드카드로 지우면 남의 데이터까지 날아가므로 반드시 범위를 좁힌다.

`players.json`에서 이번 부하테스트의 `gameId`와 `userId`만 뽑아 그 키만 지운다.

```bash
# 이번 테스트가 만든 gameId / userId 목록
GAME_IDS=$(jq -r '[.[].gameId] | unique | .[]' loadtest/players.json)
USER_IDS=$(jq -r '[.[].userId] | unique | .[]' loadtest/players.json)

for g in $GAME_IDS; do
  redis-cli --scan --pattern "game:${g}:*" | xargs -r redis-cli DEL
done

for u in $USER_IDS; do
  redis-cli DEL "refresh_token:${u}"
done
```

> ⚠️ **절대 하면 안 되는 것**
>
> - `redis-cli --scan --pattern 'game:*' | xargs redis-cli DEL`
>   → 진행 중인 다른 게임의 참가자 캐시까지 지워서, 그 게임들이 전부
>     `PARTICIPANT_NOT_FOUND`로 깨진다
> - `redis-cli --scan --pattern 'refresh_token:*' | xargs redis-cli DEL`
>   → 개발자 전원이 로그아웃된다
> - `redis-cli DEL 'redisson_delay_queue:{game:schedule:events}'`
>   → **공유 큐다.** 다른 게임의 `POLICE_MOVE_START` / `ROBBER_LOCATION_REVEAL` /
>     `GAME_OVER` 예약까지 사라져서 그 게임들이 영원히 끝나지 않는다

### 예약 이벤트는 어떻게 하나

지연 큐에는 삭제된 게임의 이벤트가 남는다. 이것들이 발화하면 `GameEventConsumer`가
게임을 못 찾아 에러 로그를 남기지만, **그뿐이고 다른 게임에 영향은 없다.**
`consume()`이 `while` 루프 안에서 `catch (Exception)`으로 받고 계속 돌기 때문이다.
라운드 시간(기본 30분)이 지나면 자연히 소진되므로 **그냥 두는 게 맞다.**

로그 노이즈까지 피하고 싶으면 공유 Redis를 쓰지 말고, 부하테스트용 Redis를
**별도 포트로 따로 띄워** `REDIS_HOST` / `REDIS_PORT`를 그쪽으로 돌린다.
그러면 끝나고 `FLUSHALL` 한 번으로 정리된다.

---

## 6. 테스트 끝나고 되돌릴 것

§5의 데이터 정리와 별개로, 켜둔 스위치를 내린다.

- [ ] `LOADTEST_SEED_ENABLED` 내리기 — `run --rm`으로 주입했다면 컨테이너가 사라지면서 자동으로 정리된다
- [ ] `--loadtest.stats.logging-period-ms` 빼기 — 안 빼면 10초마다 브로커 통계가 찍힌다
- [ ] `players.json` 삭제 — **유효한 access token 170개가 들어있다**
- [ ] 시딩 데이터 SQL 정리 (§5)
- [ ] Redis 키 정리 (§5) — 범위 제한 확인
- [ ] blue/green 내린 쪽 복구 (운영 검증한 경우만)

`fcmExecutor` 반환 타입 변경은 지표 노출을 위한 것이라 되돌리지 않는다.

---

## 7. 주의

- **부하 발생기는 서버 밖(로컬 맥)에서 돌린다.** 같은 인스턴스에서 돌리면
  그 CPU도 크레딧에서 빠져나가 측정이 오염된다.
- 운영 검증 시에는 blue/green 중 한쪽을 반드시 내린다. 1 GiB에 JVM 2개는 버티지 못한다.
- `players.json`과 `loadtest/out/`은 `.gitignore`에 있다. **커밋하지 않는다.**
