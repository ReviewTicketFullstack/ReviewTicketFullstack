import { NavLink, useNavigate } from "react-router-dom";
import { Store, UtensilsCrossed, MessageSquareHeart, LogOut } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuth } from "@/app/providers";
import { useStoreLogo } from "../StoreLogoContext";

interface NavItem {
  to: string;
  label: string;
  Icon: LucideIcon;
}

const NAV_ITEMS: NavItem[] = [
  { to: "/stores", label: "가게관리", Icon: Store },
  { to: "/menu", label: "메뉴관리", Icon: UtensilsCrossed },
  { to: "/reviews", label: "리뷰관리", Icon: MessageSquareHeart },
];

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  `flex h-11 items-center gap-3 rounded-lg px-3 text-sm font-semibold transition-colors ${
    isActive
      ? "bg-brand-50 text-brand-800"
      : "text-ink-700 hover:bg-fill-100 active:bg-line-100"
  }`;

export function Sidebar() {
  const { user, signout } = useAuth();
  const { logo } = useStoreLogo();
  const navigate = useNavigate();

  const handleLogout = () => {
    signout();
    navigate("/onboarding", { replace: true });
  };

  // FE-2.1: 사이드바에 사장님 이름 표시, 가게관리/메뉴관리/리뷰관리 메뉴
  return (
    <aside className="flex w-60 flex-col gap-8 border-r border-line-100 bg-surface p-5">
      <div className="flex items-center gap-2">
        <img src="/logo.svg" alt="리뷰티켓" className="h-10 w-auto" />
        <span className="text-base font-bold text-ink-900">사장님</span>
      </div>

      {/* 로그인한 가게 — 로고와 이름을 한 줄로 묶어 소속을 분명히 한다 */}
      <div className="flex items-center gap-3 rounded-xl bg-fill-100 p-3">
        <div className="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-line-100">
          {logo ? (
            <img
              src={logo}
              alt=""
              className="h-full w-full object-cover"
            />
          ) : (
            <Store size={18} className="text-ink-500" aria-hidden="true" />
          )}
        </div>
        <span className="min-w-0 flex-1 truncate text-sm font-bold text-ink-900">
          {user?.displayName}
        </span>
      </div>

      <nav className="flex flex-col gap-1">
        {NAV_ITEMS.map(({ to, label, Icon }) => (
          <NavLink key={to} to={to} className={navLinkClassName}>
            <Icon size={18} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>

      <button
        type="button"
        onClick={handleLogout}
        className="mt-auto flex h-11 items-center gap-3 rounded-lg px-3 text-sm font-semibold text-ink-500 transition-colors hover:bg-fill-100 active:bg-line-100"
      >
        <LogOut size={18} aria-hidden="true" />
        로그아웃
      </button>
    </aside>
  );
}
