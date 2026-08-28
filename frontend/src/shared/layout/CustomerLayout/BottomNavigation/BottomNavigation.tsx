import { NavLink } from "react-router-dom";

export function BottomNavigation() {
  return (
    <nav className="sticky bottom-0 flex h-16 bg-white border-t border-line-100">
      <NavLink
        to="/home"
        className={({ isActive }) =>
          `flex-1 flex items-center justify-center text-xs font-semibold ${isActive ? "text-brand-800" : "text-ink-500"}`
        }
      >
        홈
      </NavLink>
      <NavLink
        to="/order-history"
        className={({ isActive }) =>
          `flex-1 flex items-center justify-center text-xs font-semibold ${isActive ? "text-brand-800" : "text-ink-500"}`
        }
      >
        주문내역
      </NavLink>
      <NavLink
        to="/reviews"
        className={({ isActive }) =>
          `flex-1 flex items-center justify-center text-xs font-semibold ${isActive ? "text-brand-800" : "text-ink-500"}`
        }
      >
        리뷰
      </NavLink>
    </nav>
  );
}
