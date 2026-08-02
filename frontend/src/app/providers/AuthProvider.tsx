import { createContext, useContext, useState, type ReactNode } from "react";
import type { User, UserRole, AuthContextType } from "@/entities/user";

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [selectedRole, setSelectedRole] = useState<UserRole | null>(null);

  const signin = (user: User) => {
    setUser(user);
    setSelectedRole(null); // 선택 상태 초기화 (계속 기억하려면 user.role)
  };

  const signout = () => {
    setUser(null);
    setSelectedRole(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        selectedRole,
        setSelectedRole,
        signin,
        signout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context)
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  return context;
}
