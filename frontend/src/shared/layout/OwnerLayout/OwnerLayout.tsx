import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { StoreLogoProvider } from './StoreLogoContext';

export function OwnerLayout() {
  return (
    <StoreLogoProvider>
      <div className="flex min-h-screen">
        <Sidebar />
        {/* 자식 라우트(가게관리/메뉴관리/리뷰관리 페이지)가 여기 렌더링됨 */}
        <main className="flex-1">
          <Outlet />
        </main>
      </div>
    </StoreLogoProvider>
  );
}
