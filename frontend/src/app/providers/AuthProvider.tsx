import { createContext, useContext, useState, type ReactNode } from "react";
import type { User, UserRole, AuthContextType } from "@/entities/user";

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [userRole, setUserRole] = useState<UserRole | null>(null);

  const signin = (user: User) => {
    setUser(user);
    setUserRole(user.role);
  };

  const signout = () => {
    setUser(null);
    setUserRole(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        userRole,
        setUserRole,
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
