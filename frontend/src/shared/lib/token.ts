/**
 * 로그인 토큰 보관.
 *
 * localStorage 를 쓰는 이유 — 새로고침해도 로그인이 풀리지 않아야 한다.
 * 저장이 막히는 환경(사생활 보호 모드 등)에서는 메모리로 물러나고,
 * 이 경우 탭을 닫으면 로그인이 풀린다.
 */

const TOKEN_KEY = "reviewticket:token";
const EXPIRES_AT_KEY = "reviewticket:token-expires-at";

let memoryToken: string | null = null;
let memoryExpiresAt = 0;

export function saveToken(token: string, expiresInSeconds: number) {
  const expiresAt = Date.now() + expiresInSeconds * 1000;

  memoryToken = token;
  memoryExpiresAt = expiresAt;

  try {
    window.localStorage.setItem(TOKEN_KEY, token);
    window.localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));
  } catch {
    // 메모리에는 남아 있으므로 현재 탭에서는 그대로 쓸 수 있다.
  }
}

export function getToken(): string | null {
  const token = readToken();
  if (!token) return null;

  // 만료된 토큰은 서버가 어차피 401 로 돌려준다. 미리 지워 헛된 요청을 줄인다.
  const expiresAt = readExpiresAt();
  if (expiresAt > 0 && Date.now() >= expiresAt) {
    clearToken();
    return null;
  }

  return token;
}

export function clearToken() {
  memoryToken = null;
  memoryExpiresAt = 0;

  try {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(EXPIRES_AT_KEY);
  } catch {
    // 메모리 값은 이미 비웠다.
  }
}

function readToken(): string | null {
  if (memoryToken) return memoryToken;

  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function readExpiresAt(): number {
  if (memoryExpiresAt > 0) return memoryExpiresAt;

  try {
    const stored = Number(window.localStorage.getItem(EXPIRES_AT_KEY));
    return Number.isFinite(stored) ? stored : 0;
  } catch {
    return 0;
  }
}
