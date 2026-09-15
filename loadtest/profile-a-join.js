// 프로파일 A — 입장 폭주
// 동시 접속 시 DB 커넥션 풀 병목 확인

import ws from 'k6/ws';
import { check } from 'k6';
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
const RAMP_SECONDS = __ENV.RAMP_SECONDS || '60s';
const HOLD_SECONDS = Number(__ENV.HOLD_SECONDS || 60);

const players = new SharedArray('players', () =>
  JSON.parse(open(__ENV.PLAYERS || './players.json'))
);

// CONNECT 왕복. 서버의 유저 조회 1회가 여기 포함된다.
const connectDuration = new Trend('stomp_connect_duration', true);
// CONNECTED 수신 → 자기 채팅이 되돌아올 때까지. 구독 7건이 실제로 붙었음을 증명한다.
const subscribeDuration = new Trend('stomp_subscribe_duration', true);
const joinSuccess = new Rate('join_success');
const stompErrors = new Counter('stomp_error_frames');

export const options = {
  scenarios: {
    join_burst: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP_SECONDS, target: players.length },
        { duration: `${HOLD_SECONDS}s`, target: players.length },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    join_success: ['rate>0.99'],
    stomp_error_frames: ['count==0'],
    stomp_connect_duration: ['p(95)<2000'],
    stomp_subscribe_duration: ['p(95)<3000'],
  },
};

export default function () {
  const player = players[(exec.vu.idInTest - 1) % players.length];
  const marker = `join-${exec.vu.idInTest}-${Date.now()}`;

  let openedAt = 0;
  let connectedAt = 0;
  let joined = false;

  const res = ws.connect(WS_URL, {}, function (socket) {
    socket.on('open', () => {
      openedAt = Date.now();
      socket.send(connectFrame(player.token));
    });

    socket.on('message', (raw) => {
      for (const frame of parseFrames(raw)) {
        if (frame.command === 'CONNECTED') {
          connectedAt = Date.now();
          connectDuration.add(connectedAt - openedAt, { room: player.room });

          gameSubscriptions(player.gameId, player.team).forEach((dest, i) => {
            socket.send(subscribeFrame(i, dest));
          });

          // 구독이 실제로 붙었는지는 자기 메시지가 되돌아오는 것으로 확인한다.
          socket.send(
            sendFrame(`/publish/game/${player.gameId}/chat`, {
              message: marker,
              scope: 'ALL',
            })
          );

          // 도둑은 입장 직후 조건 없이 위치 1회를 보낸다 (FE 정책, 계획 §2).
          if (player.team === 'ROBBER') {
            socket.send(
              sendFrame(`/publish/game/${player.gameId}/location`, jitterLocation())
            );
          }

          socket.setInterval(() => socket.send(HEARTBEAT), 8000);
        } else if (frame.command === 'MESSAGE') {
          if (!joined && frame.body.indexOf(marker) !== -1) {
            joined = true;
            subscribeDuration.add(Date.now() - connectedAt, { room: player.room });
            joinSuccess.add(true);
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

    socket.on('close', () => {
      if (!joined) {
        joinSuccess.add(false);
      }
    });

    socket.setTimeout(() => socket.close(), HOLD_SECONDS * 1000);
  });

  check(res, { 'ws handshake 101': (r) => r && r.status === 101 });
}
