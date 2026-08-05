import { clearToken, getToken } from "@/shared/lib/token";

const API_BASE_URL = "/api";

/** 서버 공통 에러 응답. 백엔드 ApiExceptionHandler 가 이 형태로만 내보낸다. */
interface ApiErrorBody {
  error?: string;
  message?: string;
  retryable?: boolean;
}

/**
 * 서버가 돌려준 message 를 그대로 들고 다니는 에러.
 * 화면에서 상태 코드를 다시 해석하지 않고 message 만 보여주면 된다.
 */
export class ApiError extends Error {
  readonly status: number;
  /** 잠시 후 같은 요청을 다시 보내볼 만한지 (429, 네트워크 실패 등). */
  readonly retryable: boolean;

  constructor(message: string, status: number, retryable = false) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.retryable = retryable;
  }
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  body?: unknown;
  query?: Record<string, string>;
  /** 토큰을 실어 보낼지. 토큰이 없으면 요청하지 않고 401 로 끊는다. */
  auth?: boolean;
  signal?: AbortSignal;
}

export async function request<T>(
  path: string,
  { method = "GET", body, query, auth = false, signal }: RequestOptions = {},
): Promise<T> {
  const headers: Record<string, string> = {};

  if (auth) {
    const token = getToken();
    if (!token) {
      throw new ApiError("로그인이 필요합니다. 다시 로그인해 주세요.", 401);
    }
    headers.Authorization = `Bearer ${token}`;
  }

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    // 요청을 취소한 것은 실패가 아니다 — 호출한 쪽이 그대로 구분할 수 있게 넘긴다.
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new ApiError("서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.", 0, true);
  }

  if (!response.ok) {
    // 만료·폐기된 토큰을 남겨두면 이후 모든 요청이 같은 401 을 반복한다.
    if (response.status === 401 && auth) clearToken();
    throw await toApiError(response);
  }

  return (await readBody(response)) as T;
}

function buildUrl(path: string, query?: Record<string, string>) {
  if (!query) return `${API_BASE_URL}${path}`;
  return `${API_BASE_URL}${path}?${new URLSearchParams(query).toString()}`;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body: ApiErrorBody = await response.json();
    return new ApiError(
      body.message || fallbackMessage(response.status),
      response.status,
      Boolean(body.retryable),
    );
  } catch {
    return new ApiError(fallbackMessage(response.status), response.status);
  }
}

function fallbackMessage(status: number) {
  if (status === 401) return "로그인이 필요합니다. 다시 로그인해 주세요.";
  if (status === 403) return "권한이 없습니다.";
  if (status === 404) return "요청한 정보를 찾을 수 없습니다.";
  if (status === 429) return "요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요.";
  return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}

/** 본문 없이 200 만 돌려주는 응답(POST /auth/resend)도 그대로 통과시킨다. */
async function readBody(response: Response) {
  if (response.status === 204) return undefined;

  const text = await response.text();
  if (!text) return undefined;

  return JSON.parse(text);
}
