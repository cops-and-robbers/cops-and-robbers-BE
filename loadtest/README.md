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

```bash
# 로컬 인프라 (postgres-main 5432, redis-main 6379)
docker compose -f docker-compose-dev.yml up -d postgres-main redis-main

# 시딩 플래그를 켜고 부팅
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun \
  --args='--loadtest.seed.enabled=true --loadtest.seed.output=./loadtest/players.json'
```

로컬에서는 `logback-spring.xml`의 `LOG_PATH`가 컨테이너 경로 `/log`로 고정되어 있어
그냥 부팅하면 logback 초기화에서 실패한다. 콘솔 전용 설정으로 우회한다.

```bash
--args='--logging.config=/path/to/console-only-logback.xml --loadtest.seed.enabled=true ...'
```

### OCI 개발서버 (실행 단계 2)

서버가 이미 `SPRING_PROFILES_ACTIVE=dev`로 돌고 있으므로(`docker-compose-dev-server.yml`),
컨테이너 환경에 플래그만 얹으면 된다.

```yaml
# docker-compose-dev-server.yml 의 environment 에 임시로 추가
LOADTEST_SEED_ENABLED: "true"
LOADTEST_SEED_OUTPUT: /log/players.json
```

`/log`는 호스트의 `/home/ubuntu/log`에 마운트되어 있으므로, 생성된 `players.json`을
`scp`로 내려받아 부하 발생기(로컬 맥)에서 쓴다. **시딩이 끝나면 플래그를 반드시 다시 내린다.**

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

## 4. 서버 쪽에서 같이 볼 것

부하 발생기 지표만으로는 원인이 안 잡힌다. 아래를 병행한다.

| 대상 | 지표 | 왜 |
|---|---|---|
| HikariCP | `hikaricp_connections_pending` | 프로파일 A의 1순위 병목 |
| JVM | heap used / GC pause | 팬아웃 적체가 힙으로 나타난다 |
| WebSocket | 세션 수, `clientOutboundChannel` 큐 | 프로파일 B의 1순위 병목 |
| FCM | `fcmExecutor` 큐 깊이 | `DiscardOldestPolicy`라 적체 시 푸시가 조용히 버려진다 |
| EC2 | CPU%, `CPUCreditBalance` | 운영 검증 시에만 |

actuator + micrometer-prometheus + node-exporter가 이미 붙어 있으므로 수집 경로는 갖춰져 있다.

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
DELETE FROM game_results     WHERE game_id IN (SELECT id FROM lt_games);
DELETE FROM participants     WHERE game_id IN (SELECT id FROM lt_games);
DELETE FROM game_areas       WHERE game_id IN (SELECT id FROM lt_games);
DELETE FROM games            WHERE id      IN (SELECT id FROM lt_games);
DELETE FROM user_devices     WHERE user_id IN (SELECT id FROM lt_users);
DELETE FROM users            WHERE id      IN (SELECT id FROM lt_users);

COMMIT;
```

> `games`를 참조하는 테이블이 더 있으면 FK 제약으로 막힌다.
> 그때는 에러 메시지에 나온 테이블을 `participants` 줄 위에 추가하면 된다.

Redis 잔여 키도 함께 지운다.

```bash
redis-cli --scan --pattern 'game:*' | xargs -r redis-cli DEL
redis-cli --scan --pattern 'refresh_token:*' | xargs -r redis-cli DEL
redis-cli DEL 'redisson_delay_queue:{game:schedule:events}' \
              'redisson_delay_queue_timeout:{game:schedule:events}'
```

스케줄 큐를 안 지우면 사라진 게임의 이벤트가 계속 발화해
`GameEventConsumer`에서 에러 로그가 반복된다.

---

## 6. 주의

- **부하 발생기는 서버 밖(로컬 맥)에서 돌린다.** 같은 인스턴스에서 돌리면 그 CPU도 크레딧에서 빠져 측정이 오염된다.
- 운영 검증 시에는 blue/green 중 한쪽을 반드시 내린다. 1 GiB에 JVM 2개는 버티지 못한다.
- `players.json`은 유효한 access token을 담고 있다. **커밋하지 않는다.**
