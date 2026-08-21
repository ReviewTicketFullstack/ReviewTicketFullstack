import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/providers";
import { useStoreLogo } from "../StoreLogoContext";

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  isActive
    ? "cursor-pointer font-bold text-brand-800"
    : "cursor-pointer text-ink-700";

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
    <aside className="flex w-65 flex-col border-r border-neutral-200 bg-neutral-50 p-4">
      <div className="mb-4 flex items-center justify-center gap-2">
        <img src="/logo.svg" alt="Review Ticket" className="h-20 w-auto" />
        <span className="text-xl font-semibold text-ink-900">사장님</span>
      </div>
      <div className="mb-6 flex items-center gap-2 rounded-lg bg-neutral-200 p-3">
        <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center overflow-hidden rounded-md bg-neutral-300">
          {logo ? (
            <img
              src={logo}
              alt="가게 로고"
              className="h-full w-full object-cover"
            />
          ) : (
            <span className="text-[10px] text-neutral-500">Image</span>
          )}
        </div>
        <span className="flex-1 truncate font-semibold pl-2">
          {user?.displayName}
        </span>
      </div>
      {/* 사이드바 네비게이션 메뉴 */}
      <nav className="flex flex-col gap-5 pl-4">
        {/* /stores - 가게 목록 및 정보 관리 */}
        <NavLink to="/stores" className={navLinkClassName}>
          가게관리
        </NavLink>
        {/* /menu - 메뉴 추가/수정/삭제 */}
        <NavLink to="/menu" className={navLinkClassName}>
          메뉴관리
        </NavLink>
        {/* /reviews - 리뷰 확인 및 답글 관리 */}
        <NavLink to="/reviews" className={navLinkClassName}>
          리뷰관리
        </NavLink>
      </nav>
      {/* 로그아웃: 사이드바 하단 좌측 정렬 */}
      <button
        type="button"
        onClick={handleLogout}
        className="mt-auto cursor-pointer pl-4 text-left text-ink-500 text-sm"
      >
        로그아웃
      </button>
    </aside>
  );
}
