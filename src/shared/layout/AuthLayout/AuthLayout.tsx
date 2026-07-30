import { Outlet, useLocation } from "react-router-dom";
import { LoginHeader } from "./Header";

export function AuthLayout() {
  const location = useLocation();
  const showHeader = location.pathname !== "/onboarding";

  return (
    <div className="min-h-screen bg-surface-sub">
      <div className="relative mx-auto flex min-h-screen w-full max-w-[480px] flex-col bg-surface shadow-lg">
        {showHeader && <LoginHeader />}

        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
