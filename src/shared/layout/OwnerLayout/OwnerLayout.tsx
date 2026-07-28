import { Outlet } from 'react-router-dom';
import { Sidebar } from '../Sidebar';

export function OwnerLayout() {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
