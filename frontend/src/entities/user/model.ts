/**
 * User/Auth domain types.
 */

export type UserRole = "CUSTOMER" | "OWNER";

export interface User {
  id: number;
  email: string;
  displayName: string;
  role: UserRole;
  /** 서버가 아직 내려주지 않는 값이다. 있을 때만 쓴다. */
  createdAt?: string;
  tickets?: number;
}

/** POST /api/auth/login 응답. 이메일은 들어 있지 않다 — 로그인 폼의 입력값을 그대로 쓴다. */
export interface LoginResponse {
  token: string;
  expiresInSeconds: number;
  userId: number;
  displayName: string;
  role: UserRole;
}

export interface AuthContextType {
  user: User | null;

  selectedRole: UserRole | null; // 온보딩에서 선택한 역할

  /** 저장된 토큰으로 세션을 되살리는 중. 이때 로그인 화면으로 보내면 안 된다. */
  isRestoring: boolean;

  signin: (result: LoginResponse, email: string) => Promise<void>;
  signout: () => void;
  setSelectedRole: (role: UserRole | null) => void;
  updateDisplayName: (displayName: string) => void;
}
