import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { BottomNavigation } from './BottomNavigation';

export function CustomerLayout() {
  return (
    <div className="min-h-screen bg-gray-100">
      <div className="mx-auto flex min-h-screen w-full max-w-[860px] flex-col bg-white shadow-lg">
        <Header />

        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>

        <BottomNavigation />
      </div>
    </div>
  );
}
