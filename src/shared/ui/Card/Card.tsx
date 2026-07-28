import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  /**  전체가 터치 타깃일 때. 자동으로 button 태그와 shadow-raised 추가 */
  interactive?: boolean;
  /** 정보 카드일 때 Border 표시 */
  bordered?: boolean;
}

export function Card({
  children,
  interactive = false,
  bordered = false,
  className = '',
  ...props
}: CardProps) {
  const baseClass = 'rounded-2xl bg-surface p-3';
  const elevationClass = interactive ? 'shadow-raised' : bordered ? 'border border-line-100' : '';
  const finalClass = `${baseClass} ${elevationClass} ${className}`;

  if (interactive) {
    const buttonProps = props as ButtonHTMLAttributes<HTMLButtonElement>;
    return (
      <button
        type="button"
        className={`${finalClass} text-left transition-transform active:scale-95`}
        {...buttonProps}
      >
        {children}
      </button>
    );
  }

  return (
    <div className={finalClass} {...props}>
      {children}
    </div>
  );
}
