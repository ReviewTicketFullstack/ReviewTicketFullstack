import { BrowserRouter } from "react-router-dom";
import { AppRoutes } from "@/routes";
import { createContext, useContext, useState, type ReactNode } from "react";

type User = CustomerUser | OwnerUser;
type AuthType = "CUSTOMER" | "OWNER";
interface CustomerUser {
  uid: string;
  email: string;
  nickname: string;
  createdAt: string;
  role: "CUSTOMER";
}

interface OwnerUser {
  uid: string;
  email: string;
  storeName: string;
  createdAt: string;
  role: "OWNER";
}
interface AuthContextType {
  user: User | null;
  authType: AuthType | null;

  signin: (user: User) => void;
  signout: () => void;
  setAuthType: (type: AuthType | null) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [authType, setAuthType] = useState<AuthType | null>(null);
  const signin = (user: User) => {
    setUser(user);
  };

  const signout = () => {
    setUser(null);
    setAuthType(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        authType,
        setAuthType,
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

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen bg-gray-100">
          <AppRoutes />
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
