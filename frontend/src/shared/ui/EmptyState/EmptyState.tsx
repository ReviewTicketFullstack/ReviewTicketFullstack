import type { ReactNode } from 'react';

export interface EmptyStateProps {
  /**  설명 문구 */
  message: string;
  /** 선택사항: 다음 행동 제시 */
  action?: ReactNode;
}

export function EmptyState({ message, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-10 text-center">
      <p className="text-sm text-ink-500">{message}</p>
      {action}
    </div>
  );
}
