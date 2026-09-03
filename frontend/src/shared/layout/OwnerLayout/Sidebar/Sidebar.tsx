import { NavLink } from 'react-router-dom';

export function Sidebar() {
  return (
    <aside>
      <nav className="flex flex-col">
        <NavLink to="/owner/stores">사이드바 - 가게 관리</NavLink>
        <NavLink to="/owner/menu">사이드바 - 메뉴 관리</NavLink>
        <NavLink to="/owner/reviews">사이드바 - 리뷰관리</NavLink>
      </nav>
    </aside>
  );
}
