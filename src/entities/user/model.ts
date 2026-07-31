/**
 * User/Auth domain types.
 */

export type UserRole = "CUSTOMER" | "OWNER";

export interface CustomerUser {
  uid: string;
  email: string;
  nickname: string;
  createdAt: string;
  role: "CUSTOMER";
}

export interface OwnerUser {
  uid: string;
  email: string;
  storeName: string;
  createdAt: string;
  role: "OWNER";
}

export type User = CustomerUser | OwnerUser;

export interface AuthContextType {
  user: User | null;
  userRole: UserRole | null;

  signin: (user: User) => void;
  signout: () => void;
  setUserRole: (type: UserRole | null) => void;
}
