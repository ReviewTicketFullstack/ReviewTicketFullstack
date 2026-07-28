import { useEffect, useId, useRef, type ReactNode } from 'react';

export interface ModalProps {
  open: boolean;
  onClose?: () => void;
  title?: string;
  children?: ReactNode;
  /** 액션 버튼 최대 2개 */
  actions?: ReactNode;
}

export function Modal({ open, onClose, title, children, actions }: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return;

    // 열기 직전 포커스를 기억해 두었다가 닫을 때 되돌린다.
    const previouslyFocused = document.activeElement;

    const prevOverflow = document.documentElement.style.overflow;
    document.documentElement.style.overflow = 'hidden';

    panelRef.current?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose?.();
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.documentElement.style.overflow = prevOverflow;

      if (previouslyFocused instanceof HTMLElement) {
        previouslyFocused.focus();
      }
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    // 오버레이 클릭으로 닫는다. 키보드 경로는 위의 Esc가 담당한다.
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-5"
      onClick={onClose}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? titleId : undefined}
        tabIndex={-1}
        className="w-full max-w-[400px] rounded-2xl bg-surface shadow-sheet focus:outline-none"
        onClick={(e) => e.stopPropagation()}
      >
        {title && (
          <div className="border-b border-line-100 px-4 py-3">
            <h2 id={titleId} className="text-base font-bold text-ink-900">
              {title}
            </h2>
          </div>
        )}

        {children && <div className="max-h-[60vh] overflow-y-auto px-4 py-3">{children}</div>}

        {actions && (
          <div className="flex justify-end gap-2 border-t border-line-100 px-4 py-3">{actions}</div>
        )}
      </div>
    </div>
  );
}
