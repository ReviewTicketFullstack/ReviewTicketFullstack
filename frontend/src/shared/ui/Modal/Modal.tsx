import { useEffect, type ReactNode } from "react";

export interface ModalProps {
  open: boolean;
  onClose?: () => void;
  children?: ReactNode;
}

export function Modal({ open, onClose, children }: ModalProps) {
  // 열려 있는 동안 배경 스크롤을 잠근다(design.md Modal). 닫히거나 언마운트될
  // 때 반드시 되돌린다 — 되돌리지 않으면 페이지 전체가 스크롤되지 않는다.
  useEffect(() => {
    if (!open) return;

    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose?.();
    };
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previous;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-5"
    >
      {/* 오버레이는 검정 반투명 1종. 배경 블러를 쓰지 않는다(design.md) */}
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div
        className="relative z-10 max-h-[90vh] w-full max-w-[400px] overflow-y-auto rounded-2xl bg-surface p-5 shadow-sheet"
        style={{ animation: "slideUp 0.24s cubic-bezier(0.16, 1, 0.3, 1)" }}
      >
        {children}
      </div>
    </div>
  );
}
