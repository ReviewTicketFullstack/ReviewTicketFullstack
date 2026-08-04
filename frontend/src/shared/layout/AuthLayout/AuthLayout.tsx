import { Outlet, useLocation } from "react-router-dom";
import { AuthHeader } from "./Header";

/** 경로별 헤더 제목. 여기에 없는 경로는 헤더를 표시하지 않는다. */
const HEADER_TITLES: Record<string, string> = {
  "/login": "로그인",
  "/signup": "회원가입",
};

export function AuthLayout() {
  const location = useLocation();
  const title = HEADER_TITLES[location.pathname];

  return (
    <div className="min-h-screen bg-surface-sub">
      <div className="relative mx-auto flex min-h-screen w-full max-w-[480px] flex-col bg-surface shadow-lg">
        {title && <AuthHeader title={title} />}

        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
