import { request } from "@/shared/api";
import type { UserRole } from "@/entities/user";

/** 대상 회원 번호를 보내지 않는다 — 토큰의 주체가 곧 대상이다. */
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

export function changeDisplayName(displayName: string): Promise<ChangeNameResponse> {
  return request<ChangeNameResponse>("/me/name", {
    method: "PATCH",
    body: { displayName },
    auth: true,
  });
}
