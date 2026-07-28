import { Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <main>
        <Outlet />
      </main>
    </div>
  );
}
