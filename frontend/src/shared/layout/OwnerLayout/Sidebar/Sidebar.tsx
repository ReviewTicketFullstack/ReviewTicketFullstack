import { NavLink } from 'react-router-dom';
import { useAuth } from '@/app/providers';

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'font-bold text-brand-800' : 'text-neutral-700';

export function Sidebar() {
  const { user } = useAuth();
  const storeName = user?.role === 'OWNER' ? user.storeName : '';

  return (
    <aside className="flex w-52 flex-col border-r border-neutral-200 bg-neutral-50 p-4">
      <div className="mb-6 rounded-lg bg-neutral-200 p-4 text-center font-semibold">
        {storeName || '가게 정보'}
      </div>
      <nav className="flex flex-col gap-3">
        <NavLink to="/owner/stores" className={navLinkClassName}>가게관리</NavLink>
        <NavLink to="/owner/menu" className={navLinkClassName}>메뉴관리</NavLink>
        <NavLink to="/owner/reviews" className={navLinkClassName}>리뷰관리</NavLink>
      </nav>
    </aside>
  );
}
