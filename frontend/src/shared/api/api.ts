import { clearToken, getToken } from "@/shared/lib/token";
import type { LoginResponse, UserRole } from "@/entities/user";

// ============================================================================
// HTTP Client
// ============================================================================

const API_BASE_URL = "/api";

interface ApiErrorBody {
  errorCode?: string;
  message?: string;
  retryable?: boolean;
}

export class ApiError extends Error {
  readonly status: number;
  readonly retryable: boolean;
  readonly errorCode?: string;

  constructor(
    message: string,
    status: number,
    retryable = false,
    errorCode?: string,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.retryable = retryable;
    this.errorCode = errorCode;
  }
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  body?: unknown;
  query?: Record<string, string>;
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
    if (error instanceof DOMException && error.name === "AbortError")
      throw error;
    throw new ApiError(
      "서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      0,
      true,
    );
  }

  if (!response.ok) {
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
      body.errorCode,
    );
  } catch {
    return new ApiError(fallbackMessage(response.status), response.status);
  }
}

function fallbackMessage(status: number) {
  if (status === 401) return "로그인이 필요합니다. 다시 로그인해 주세요.";
  if (status === 403) return "권한이 없습니다.";
  if (status === 404) return "요청한 정보를 찾을 수 없습니다.";
  if (status === 429)
    return "요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요.";
  return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}

async function readBody(response: Response) {
  if (response.status === 204) return undefined;

  const text = await response.text();
  if (!text) return undefined;

  return JSON.parse(text);
}

// ============================================================================
// Auth API
// ============================================================================

// TODO: mock mode 제거
const MOCK_SIGNUP_ENABLED = true;

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignUpRequest {
  email: string;
  password: string;
  passwordConfirm: string;
  role: UserRole;
  displayName: string;
}

export interface SignUpResponse {
  email: string;
  message: string;
}

export interface AvailabilityResponse {
  available: boolean;
}

export interface VerifiedResponse {
  verified: boolean;
}

// export interface MessageResponse {
//   message: string;
// }

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/auth/login", { method: "POST", body: data });
}

export async function signUp(data: SignUpRequest): Promise<SignUpResponse> {
  if (MOCK_SIGNUP_ENABLED) {
    return handleMockSignUp(data);
  }

  return request<SignUpResponse>("/auth/signup", {
    method: "POST",
    body: data,
  });
}

async function handleMockSignUp(data: SignUpRequest): Promise<SignUpResponse> {
  await new Promise((resolve) => setTimeout(resolve, 500));

  // TODO: remove mock mode
  console.groupCollapsed("[Signup] Mock Mode - Simulating response");
  console.log("Input data:", {
    email: data.email,
    role: data.role,
    displayName: data.displayName,
    passwordMatch: data.password === data.passwordConfirm,
  });

  if (data.email.toLowerCase().includes("taken")) {
    console.log("Mock result: EMAIL_TAKEN");
    console.groupEnd();
    throw new ApiError("이미 가입된 이메일입니다", 400, false, "EMAIL_TAKEN");
  }

  if (data.displayName.toLowerCase().includes("taken")) {
    console.log("Mock result: NAME_TAKEN");
    console.groupEnd();
    throw new ApiError(
      data.role === "CUSTOMER"
        ? "이미 쓰이고 있는 닉네임입니다"
        : "이미 쓰이고 있는 가게 이름입니다",
      400,
      false,
      "NAME_TAKEN",
    );
  }

  if (data.password !== data.passwordConfirm) {
    console.log("Mock result: PASSWORD_MISMATCH");
    console.groupEnd();
    throw new ApiError(
      "비밀번호가 서로 다릅니다",
      400,
      false,
      "PASSWORD_MISMATCH",
    );
  }

  if (!data.email.includes("@")) {
    console.log("Mock result: EMAIL_DOMAIN_INVALID");
    console.groupEnd();
    throw new ApiError(
      "유효하지 않은 이메일 주소입니다",
      400,
      false,
      "EMAIL_DOMAIN_INVALID",
    );
  }

  console.log("Mock result: Success");
  console.groupEnd();
  return {
    email: data.email.toLowerCase().trim(),
    message:
      "인증 메일을 보냈습니다. 메일의 링크를 눌러야 회원가입이 완료됩니다.",
  };
}

export async function checkEmail(
  email: string,
  signal?: AbortSignal,
): Promise<AvailabilityResponse> {
  if (MOCK_SIGNUP_ENABLED) {
    return handleMockCheckEmail(email);
  }

  return request<AvailabilityResponse>("/auth/check-email", {
    query: { email },
    signal,
  });
}

export async function checkName(
  name: string,
  signal?: AbortSignal,
): Promise<AvailabilityResponse> {
  if (MOCK_SIGNUP_ENABLED) {
    return handleMockCheckName(name);
  }

  return request<AvailabilityResponse>("/auth/check-name", {
    query: { name },
    signal,
  });
}

async function handleMockCheckEmail(
  email: string,
): Promise<AvailabilityResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  // TODO: remove mock mode
  console.groupCollapsed("[Signup] Mock Mode - checkEmail");
  console.log("Email:", email);

  const available = !email.toLowerCase().includes("taken");
  console.log("Available:", available);
  console.groupEnd();

  return { available };
}

async function handleMockCheckName(
  name: string,
): Promise<AvailabilityResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  // TODO: remove mock mode
  console.groupCollapsed("[Signup] Mock Mode - checkName");
  console.log("Name:", name);

  const available = !name.toLowerCase().includes("taken");
  console.log("Available:", available);
  console.groupEnd();

  return { available };
}

export function getVerificationStatus(
  email: string,
  signal?: AbortSignal,
): Promise<VerifiedResponse> {
  return request<VerifiedResponse>("/auth/status", {
    query: { email },
    signal,
  });
}

export function resendVerification(email: string): Promise<{ code: string }> {
  return request<{ code: string }>("/auth/resend", {
    method: "POST",
    query: { email },
  });
}

export function requestPasswordReset(email: string): Promise<{ code: string }> {
  return request<{ code: string }>("/auth/password-reset/request", {
    method: "POST",
    body: { email },
  });
}

// ============================================================================
// Account API
// ============================================================================

export interface MeResponse {
  userId: number;
  email: string;
  displayName: string;
  role: UserRole;
}

export interface ChangeNameResponse {
  displayName: string;
}

export function getMe(signal?: AbortSignal): Promise<MeResponse> {
  return request<MeResponse>("/me", { auth: true, signal });
}

export function changeDisplayName(
  displayName: string,
): Promise<ChangeNameResponse> {
  return request<ChangeNameResponse>("/me/name", {
    method: "PATCH",
    body: { displayName },
    auth: true,
  });
}
