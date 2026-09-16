// 프로파일 B — 게임 진행
// 메시지 팬아웃 처리량과 메모리 안정성 확인

import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';
import {
  connectFrame,
  subscribeFrame,
  sendFrame,
  parseFrames,
  gameSubscriptions,
  jitterLocation,
  HEARTBEAT,
} from './lib/stomp.js';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/connection';
const DURATION_MINUTES = Number(__ENV.DURATION_MINUTES || 15);
// 접속을 이 시간 안에 흩뿌린다. 170명이 같은 순간에 붙는 건 입장 폭주(프로파일 A)의 몫이지 여기서 볼 게 아니다.
const JOIN_SPREAD_SECONDS = Number(__ENV.JOIN_SPREAD_SECONDS || 60);

const LOCATION_INTERVAL_MS = Number(__ENV.LOCATION_INTERVAL_MS || 5000);
const PING_INTERVAL_MS = Number(__ENV.PING_INTERVAL_MS || 20000);
const CLOSE_TOLERANCE_MS = 2000;

const players = new SharedArray('players', () =>
  JSON.parse(open(__ENV.PLAYERS || './players.json'))
);

const roomSizes = players.reduce((acc, p) => {
  acc[p.room] = (acc[p.room] || 0) + 1;
  return acc;
}, {});

// 방 크기별로 분리해서 본다. 20명 방은 멀쩡한데 100명 방만 느려지면 원인이 팬아웃으로 특정된다.
const chatRtt = new Trend('stomp_chat_rtt', true);
const chatSent = new Counter('stomp_chat_sent');
const messagesReceived = new Counter('stomp_messages_received');
const locationSent = new Counter('stomp_location_sent');
const stompErrors = new Counter('stomp_error_frames');
const sessionAlive = new Rate('session_alive');

export const options = {
  scenarios: {
    play: {
      executor: 'per-vu-iterations',
      vus: players.length,
      iterations: 1,
      maxDuration: `${DURATION_MINUTES * 60 + JOIN_SPREAD_SECONDS + 120}s`,
    },
  },
  thresholds: {
    'stomp_chat_rtt{room:A}': ['p(95)<500'],
    'stomp_chat_rtt{room:B}': ['p(95)<500'],
    'stomp_chat_rtt{room:C}': ['p(95)<500'],
    stomp_error_frames: ['count==0'],
    session_alive: ['rate>0.99'],
  },
};

/**
 * 경과 시간에 따라 B방의 목표 채팅 레이트(방 전체 msg/s)를 정한다.
 * 전체 구간을 3등분해 1 → 5 → 10으로 올린다.
 */
function targetChatRate(elapsedMs) {
  const third = (DURATION_MINUTES * 60 * 1000) / 3;
  if (elapsedMs < third) return 1;
  if (elapsedMs < third * 2) return 5;
  return 10;
}

/** 방 전체 목표 레이트를 인원수로 나눠 VU 한 명의 전송 간격(ms)을 구한다. */
function chatIntervalMs(room, elapsedMs) {
  const size = roomSizes[room] || 1;
  // A, C방은 배경 부하만 준다. 부하의 주인공은 B방이다.
  const roomRate = room === 'B' ? targetChatRate(elapsedMs) : 1;
  return Math.max(1000, Math.round((size / roomRate) * 1000));
}

export default function () {
  const player = players[(exec.vu.idInTest - 1) % players.length];
  const marker = `b-${exec.vu.idInTest}`;
  sleep(Math.random() * JOIN_SPREAD_SECONDS);
  const startedAt = Date.now();
  const plannedEndAt = startedAt + DURATION_MINUTES * 60 * 1000;

  let connected = false;

  const res = ws.connect(WS_URL, {}, function (socket) {
    socket.on('open', () => socket.send(connectFrame(player.token)));

    socket.on('message', (raw) => {
      for (const frame of parseFrames(raw)) {
        if (frame.command === 'CONNECTED') {
          connected = true;

          gameSubscriptions(player.gameId, player.team).forEach((dest, i) => {
            socket.send(subscribeFrame(i, dest));
          });

          startLoops(socket, player, marker, startedAt);
        } else if (frame.command === 'MESSAGE') {
          messagesReceived.add(1, { room: player.room });

          // 자기가 보낸 채팅이 되돌아온 시각으로 왕복 지연을 잰다.
          // 발신과 수신이 같은 VU라 시계가 동일해 보정이 필요 없다.
          const match = frame.body.match(new RegExp(`${marker}:(\\d+)`));
          if (match) {
            chatRtt.add(Date.now() - Number(match[1]), { room: player.room });
          }
        } else if (frame.command === 'ERROR') {
          stompErrors.add(1, { room: player.room });
          console.error(`STOMP ERROR [${player.room}] ${frame.headers.message || ''} ${frame.body}`);
        }
      }
    });

    socket.on('error', (e) => {
      if (e.error() !== 'websocket: close sent') {
        console.error(`ws error [${player.room}] ${e.error()}`);
      }
    });

    // 판정은 종료 시점에 한 번만 한다.
    // CONNECTED 직후에 true 를 찍어두면, 서버가 곧바로 끊어도 실패가 기록되지 않아
    // 전 세션이 1초 만에 죽어도 임계값이 통과해버린다.
    socket.on('close', () => {
      const heldToEnd = connected && Date.now() >= plannedEndAt - CLOSE_TOLERANCE_MS;
      sessionAlive.add(heldToEnd);
    });

    socket.setTimeout(() => socket.close(), DURATION_MINUTES * 60 * 1000);
  });

  check(res, { 'ws handshake 101': (r) => r && r.status === 101 });
}

function startLoops(socket, player, marker, startedAt) {
  socket.setInterval(() => socket.send(HEARTBEAT), 8000);

  if (player.team === 'ROBBER') {
    // 도둑만 위치를 보낸다. 경찰은 게임 중 위치를 서버로 보내지 않는다 (계획 §2).
    socket.setInterval(() => {
      socket.send(sendFrame(`/publish/game/${player.gameId}/location`, jitterLocation()));
      locationSent.add(1, { room: player.room });
    }, LOCATION_INTERVAL_MS);
  } else {
    socket.setInterval(() => {
      socket.send(
        sendFrame(`/publish/game/${player.gameId}/ping`, {
          pingType: 'SUSPECT',
          location: jitterLocation(),
        })
      );
    }, PING_INTERVAL_MS);
  }

  // 채팅 레이트가 구간마다 바뀌므로 고정 interval 대신 1초마다 조건을 다시 평가한다.
  let nextChatAt = Date.now() + Math.random() * chatIntervalMs(player.room, 0);
  socket.setInterval(() => {
    const now = Date.now();
    if (now < nextChatAt) {
      return;
    }
    const elapsed = now - startedAt;
    // ALL과 TEAM을 반반 섞는다. TEAM은 팬아웃이 절반이라 비용이 다르다.
    const scope = Math.random() < 0.5 ? 'ALL' : 'TEAM';
    socket.send(
      sendFrame(`/publish/game/${player.gameId}/chat`, {
        message: `${marker}:${now}`,
        scope,
      })
    );
    chatSent.add(1, { room: player.room, scope });
    nextChatAt = now + chatIntervalMs(player.room, elapsed);
  }, 1000);
}
