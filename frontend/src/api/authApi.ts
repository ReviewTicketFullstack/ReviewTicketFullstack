import { request } from "@/shared/api";
import type { LoginResponse, UserRole } from "@/entities/user";

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

/** 가입 요청 시점에는 회원이 아직 없다 — 메일 인증을 해야 만들어진다. */
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

export interface MessageResponse {
  message: string;
}

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/auth/login", { method: "POST", body: data });
}

export function signUp(data: SignUpRequest): Promise<SignUpResponse> {
  return request<SignUpResponse>("/auth/signup", {
    method: "POST",
    body: data,
  });
}

export function checkEmail(
  email: string,
  signal?: AbortSignal,
): Promise<AvailabilityResponse> {
  return request<AvailabilityResponse>("/auth/check-email", {
    query: { email },
    signal,
  });
}

/** 고객 닉네임과 사장 가게 이름이 같은 이름 공간이라 서버 API 도 하나다. */
export function checkName(
  name: string,
  signal?: AbortSignal,
): Promise<AvailabilityResponse> {
  return request<AvailabilityResponse>("/auth/check-name", {
    query: { name },
    signal,
  });
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

export function resendVerification(email: string): Promise<MessageResponse> {
  return request<MessageResponse>("/auth/resend", {
    method: "POST",
    query: { email },
  });
}

/** 가입되지 않은 이메일이어도 항상 성공한다 — 가입 여부가 새어나가지 않게 하려는 설계다. */
export function requestPasswordReset(
  email: string,
  role: UserRole,
): Promise<MessageResponse> {
  return request<MessageResponse>("/auth/password-reset/request", {
    method: "POST",
    body: { email, role },
  });
}
