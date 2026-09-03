import { Outlet, useLocation } from "react-router-dom";
import { Header } from "./Header";
import { BottomNavigation } from "./BottomNavigation";

export function CustomerLayout() {
  const location = useLocation();

  const showBottomNavigation =
    location.pathname === "/home" || location.pathname === "/order-history";

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="relative mx-auto flex min-h-screen w-full max-w-[860px] flex-col bg-gray-50 shadow-lg">
        <Header />

        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>

        {showBottomNavigation && <BottomNavigation />}
      </div>
    </div>
  );
}
