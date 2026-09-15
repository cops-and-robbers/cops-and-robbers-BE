// STOMP 1.2 프레임 유틸
// k6에서 STOMP 클라이언트 없이 직접 처리

const NULL = '\u0000';

export function buildFrame(command, headers, body) {
  let frame = command + '\n';
  for (const [key, value] of Object.entries(headers || {})) {
    frame += `${key}:${value}\n`;
  }
  return frame + '\n' + (body || '') + NULL;
}

/**
 * raw 메시지를 NULL 기준으로 분리
 */
export function parseFrames(raw) {
  return raw
    .split(NULL)
    .map((chunk) => chunk.replace(/^\n+/, ''))
    .filter((chunk) => chunk.length > 0)
    .map((chunk) => {
      const separator = chunk.indexOf('\n\n');
      const head = separator === -1 ? chunk : chunk.slice(0, separator);
      const body = separator === -1 ? '' : chunk.slice(separator + 2);

      const lines = head.split('\n');
      const command = lines.shift();
      const headers = {};
      for (const line of lines) {
        const idx = line.indexOf(':');
        if (idx > 0) {
          headers[line.slice(0, idx)] = line.slice(idx + 1);
        }
      }
      return { command, headers, body };
    });
}

export function connectFrame(token) {
  return buildFrame('CONNECT', {
    'accept-version': '1.2',
    'heart-beat': '10000,10000',
    Authorization: `Bearer ${token}`,
  });
}

export function subscribeFrame(id, destination) {
  return buildFrame('SUBSCRIBE', { id: `sub-${id}`, destination });
}

export function sendFrame(destination, payload) {
  return buildFrame(
    'SEND',
    { destination, 'content-type': 'application/json' },
    JSON.stringify(payload)
  );
}

// 하트비트 (연결 유지용)
export const HEARTBEAT = '\n';

/**
 * 게임 입장 시 구독 채널 목록
 */
export function gameSubscriptions(gameId, team) {
  const t = team === 'POLICE' ? 'police' : 'robber';
  return [
    `/subscribe/game/${gameId}/lobby`,
    `/subscribe/game/${gameId}/system`,
    `/subscribe/game/${gameId}/system/footprint`,
    `/subscribe/game/${gameId}/chat/all`,
    `/subscribe/game/${gameId}/chat/${t}`,
    `/subscribe/game/${gameId}/location/${t}`,
    `/subscribe/game/${gameId}/ping/${t}`,
  ];
}

/**
 * 위치를 랜덤으로 약간 흔듦
 * - 10m 이상 이동 조건 충족용
 */
export function jitterLocation() {
  return {
    latitude: 37.5665 + (Math.random() - 0.5) * 0.004,
    longitude: 126.978 + (Math.random() - 0.5) * 0.004,
  };
}
