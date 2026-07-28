import type { ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'secondary' | 'ghost';
type Size = 'md' | 'sm';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  /** 파괴적 액션. primary에 Danger 색을 얹는다. */
  danger?: boolean;
}

/**
 * hover는 투명도가 아니라 한 단계 어두운 색으로 만든다.
 * 투명도를 쓰면 라벨까지 흐려져 disabled처럼 읽히고, 유채색 알파 금지 규칙에도 어긋난다.
 */
function getVariantStyles(variant: Variant, danger: boolean): string {
  if (danger && variant === 'primary') {
    return 'bg-brand-900 text-white hover:bg-brand-950 disabled:bg-ink-300 disabled:text-white';
  }

  switch (variant) {
    case 'primary':
      return 'bg-brand-800 text-white hover:bg-brand-900 disabled:bg-ink-300 disabled:text-white';
    case 'secondary':
      return 'bg-fill-100 text-ink-900 hover:bg-line-100 disabled:bg-fill-100 disabled:text-ink-300';
    case 'ghost':
      return 'bg-transparent text-ink-900 hover:bg-fill-100 disabled:text-ink-300';
  }
}

export function Button({
  variant = 'primary',
  size = 'md',
  danger = false,
  disabled = false,
  className = '',
  children,
  ...props
}: ButtonProps) {
  const sizeClass = size === 'md' ? 'h-11 px-4' : 'h-9 px-3';
  const baseClass =
    'rounded-lg text-xs font-semibold transition-colors duration-150 disabled:cursor-not-allowed focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-800';

  return (
    <button
      disabled={disabled}
      className={`${baseClass} ${sizeClass} ${getVariantStyles(variant, danger)} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
