import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type {
  User,
  UserRole,
  LoginResponse,
  AuthContextType,
} from "@/entities/user";
import { getMe } from "@/api/accountApi";
import { clearToken, getToken, saveToken } from "@/shared/lib/token";

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [selectedRole, setSelectedRole] = useState<UserRole | null>(null);
  // 토큰이 있을 때만 복원을 시도한다. 없으면 처음부터 로그아웃 상태다.
  const [isRestoring, setIsRestoring] = useState(() => Boolean(getToken()));

  useEffect(() => {
    if (!getToken()) return;

    const controller = new AbortController();

    getMe(controller.signal)
      .then((me) => {
        setUser({
          id: me.userId,
          email: me.email,
          displayName: me.displayName,
          role: me.role,
          tickets: me.tickets,
        });
      })
      .catch(() => {
        // 로그아웃 상태로 시작하면 그만이다. 여기서 토큰을 지우지는 않는다 —
        // 요청 취소(StrictMode 의 이중 마운트)나 일시적인 네트워크 오류까지
        // 여기로 오기 때문에, 지우면 멀쩡한 로그인이 풀린다.
        // 서버가 실제로 거부한 401 은 client.ts 가 이미 지웠다.
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsRestoring(false);
      });

    return () => controller.abort();
  }, []);

  const signin = async (result: LoginResponse) => {
    saveToken(result.token, result.expiresInSeconds);

    const data = await getMe();

    setUser({
      id: data.userId,
      email: data.email,
      displayName: data.displayName,
      role: data.role,
      tickets: data.tickets,
    });
    setSelectedRole(null); // 선택 상태 초기화 (계속 기억하려면 user.role)
  };

  const signout = () => {
    clearToken();
    setUser(null);
    setSelectedRole(null);
  };

  const updateDisplayName = (displayName: string) => {
    setUser((current) => (current ? { ...current, displayName } : current));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        selectedRole,
        isRestoring,
        setSelectedRole,
        signin,
        signout,
        updateDisplayName,
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
