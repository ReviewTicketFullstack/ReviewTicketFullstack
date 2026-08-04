import { NavLink } from 'react-router-dom';
import { useAuth } from '@/app/providers';

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'font-bold text-brand-800' : 'text-neutral-700';

export function Sidebar() {
  const { user } = useAuth();

  return (
    <aside className="flex w-65 flex-col border-r border-neutral-200 bg-neutral-50 p-4">
      <div className="mb-4 flex items-center justify-center gap-2">
        <img src="/logo.svg" alt="Review Ticket" className="h-20 w-auto" />
        <span className="text-xl font-semibold text-ink-900">사장님</span>
      </div>
      <div className="mb-6 flex items-center gap-2 rounded-lg bg-neutral-200 p-3">
        <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-md bg-neutral-300">
          <span className="text-[10px] text-neutral-500">Image</span>
        </div>
        <span className="flex-1 truncate font-semibold">{user?.displayName}</span>
      </div>
      <nav className="flex flex-col gap-5">
        <NavLink to="/stores" className={navLinkClassName}>가게관리</NavLink>
        <NavLink to="/menu" className={navLinkClassName}>메뉴관리</NavLink>
        <NavLink to="/reviews" className={navLinkClassName}>리뷰관리</NavLink>
      </nav>
    </aside>
  );
}
