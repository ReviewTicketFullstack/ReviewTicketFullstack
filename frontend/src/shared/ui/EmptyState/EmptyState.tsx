import type { ReactNode } from 'react';

export interface EmptyStateProps {
  /**  설명 문구 */
  message: string;
  /** 선택사항: 다음 행동 제시 */
  action?: ReactNode;
  /** 선택사항: 문구 위에 얹는 이모지·아이콘. 장식이라 스크린리더는 건너뛴다 */
  icon?: ReactNode;
}

export function EmptyState({ message, action, icon }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-5 py-12 text-center">
      {icon && (
        <span
          aria-hidden="true"
          className="flex size-14 items-center justify-center rounded-full bg-fill-100 text-2xl"
        >
          {icon}
        </span>
      )}
      <p className="text-sm leading-relaxed text-ink-500">{message}</p>
      {action}
    </div>
  );
}
