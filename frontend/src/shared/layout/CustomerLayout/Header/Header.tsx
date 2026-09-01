import { useEffect, useState } from "react";
import { Ticket, Settings } from "lucide-react";
import { useAuth } from "@/app/providers";
import { CountBadge } from "@/shared/ui";
import { MyInfoModal } from "@/pages/customer/MyInfoModal/MyInfoModal";

/** 헤더 높이(56px). 이 지점을 넘겨 스크롤됐을 때만 그림자를 노출한다. */
const HEADER_HEIGHT = 56;

export function Header() {
  const [isMyInfoModalOpen, setIsMyInfoModalOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const { user } = useAuth();

  // design.md — 헤더 그림자는 폭이 아니라 스크롤 위치로 분기하는 유일한 규칙이다.
  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > HEADER_HEIGHT);

    handleScroll();
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const iconButtonClass =
    "relative flex size-11 items-center justify-center rounded-lg text-ink-900 transition-colors hover:bg-fill-100 active:bg-line-100";

  return (
    <>
      <header
        className={`sticky top-0 z-40 h-14 bg-surface transition-shadow duration-200 ${
          isScrolled ? "shadow-header" : ""
        }`}
      >
        <div className="flex h-full items-center justify-between px-5">
          <h1 className="text-xl font-bold text-ink-900">
            리뷰<span className="text-brand-800">티켓</span>
          </h1>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => setIsMyInfoModalOpen(true)}
              className={iconButtonClass}
              aria-label={`남은 티켓 ${user?.tickets ?? 0}개, 내 정보 열기`}
            >
              <Ticket size={22} aria-hidden="true" />
              <span className="absolute right-1 top-1">
                <CountBadge count={user?.tickets ?? 0} />
              </span>
            </button>

            <button
              type="button"
              onClick={() => setIsMyInfoModalOpen(true)}
              className={iconButtonClass}
              aria-label="내 정보"
            >
              <Settings size={22} aria-hidden="true" />
            </button>
          </div>
        </div>
      </header>

      <MyInfoModal
        open={isMyInfoModalOpen}
        onClose={() => setIsMyInfoModalOpen(false)}
      />
    </>
  );
}
