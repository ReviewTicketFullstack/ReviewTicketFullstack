import { NavLink } from "react-router-dom";
import { Home, Receipt, MessageSquareHeart } from "lucide-react";
import type { LucideIcon } from "lucide-react";

interface Tab {
  to: string;
  label: string;
  Icon: LucideIcon;
}

// design.md Bottom Navigation — 탭은 최대 5개, 각 탭은 아이콘 + 라벨 1줄.
const TABS: Tab[] = [
  { to: "/home", label: "홈", Icon: Home },
  { to: "/order-history", label: "주문내역", Icon: Receipt },
  { to: "/reviews", label: "리뷰", Icon: MessageSquareHeart },
];

export function BottomNavigation() {
  return (
    <nav className="sticky bottom-0 z-40 bg-surface shadow-dock pb-[env(safe-area-inset-bottom)]">
      {/* NavLink 는 활성 탭에 aria-current="page" 를 자동으로 붙인다 —
          색 외의 단서를 스크린리더에도 주므로 따로 지정하지 않는다. */}
      <ul className="flex h-14 w-full">
        {TABS.map(({ to, label, Icon }) => (
          <li key={to} className="flex-1">
            <NavLink
              to={to}
              className={({ isActive }) =>
                `flex h-full flex-col items-center justify-center gap-1 transition-colors duration-150 ${
                  isActive ? "text-brand-800" : "text-ink-500"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    size={22}
                    aria-hidden="true"
                    // 활성 탭만 면(solid)으로 채워 색 외의 단서를 하나 더 준다.
                    fill={isActive ? "currentColor" : "none"}
                    strokeWidth={isActive ? 1.5 : 2}
                  />
                  <span className="text-xs font-semibold">{label}</span>
                </>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
