/**
 * User/Auth domain types.
 */

export type UserRole = "CUSTOMER" | "OWNER";

export interface User {
  id: number;
  email: string;
  displayName: string;
  role: UserRole;
  createdAt: string;
}

export interface AuthContextType {
  user: User | null;

  selectedRole: UserRole | null; // 온보딩에서 선택한 역할

  signin: (user: User) => void;
  signout: () => void;
  setSelectedRole: (role: UserRole | null) => void;
}
