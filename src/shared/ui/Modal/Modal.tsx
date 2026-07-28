import { useEffect, type ReactNode } from 'react';

export interface ModalProps {
  open: boolean;
  onClose?: () => void;
  title?: string;
  children?: ReactNode;
  /** 액션 버튼 최대 2개 */
  actions?: ReactNode;
}

export function Modal({ open, onClose, title, children, actions }: ModalProps) {
  useEffect(() => {
    if (!open) return;

    // 배경 스크롤 잠금
    const prevOverflow = document.documentElement.style.overflow;
    document.documentElement.style.overflow = 'hidden';

    // Esc 키로 닫기
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose?.();
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.documentElement.style.overflow = prevOverflow;
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-5">
      <div
        className="w-full max-w-[400px] rounded-2xl bg-surface shadow-sheet"
        role="dialog"
        aria-modal="true"
      >
        {title && (
          <div className="border-b border-line-100 px-4 py-3">
            <h2 className="text-base font-bold text-ink-900">{title}</h2>
          </div>
        )}

        {children && <div className="max-h-[60vh] overflow-y-auto px-4 py-3">{children}</div>}

        {actions && (
          <div className="flex gap-2 border-t border-line-100 px-4 py-3 justify-end">{actions}</div>
        )}
      </div>

      <button
        type="button"
        className="absolute inset-0"
        onClick={onClose}
        aria-label="모달 닫기"
      />
    </div>
  );
}
