import { Outlet, useLocation } from "react-router-dom";
import { Header } from "./Header";
import { BottomNavigation } from "./BottomNavigation";

export function CustomerLayout() {
  const location = useLocation();

  const showBottomNavigation =
    location.pathname === "/home" ||
    location.pathname === "/order-history" ||
    location.pathname === "/reviews";

  return (
    // 바깥은 Background, 안쪽 프레임은 Surface-sub 로 둬서 데스크톱에서도
    // 모바일 프레임이 화면과 구분돼 보이게 한다.
    <div className="min-h-screen bg-fill-100">
      <div className="relative mx-auto flex min-h-screen w-full max-w-[860px] flex-col bg-surface-sub shadow-flat">
        <Header />

        <main className="flex-1">
          <Outlet />
        </main>

        {showBottomNavigation && <BottomNavigation />}
      </div>
    </div>
  );
}
