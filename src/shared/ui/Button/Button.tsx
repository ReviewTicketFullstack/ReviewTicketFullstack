import type { ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'secondary' | 'ghost';
type Size = 'md' | 'sm';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  /** Danger: primary에 brand-900 색 */
  danger?: boolean;
}

function joinClasses(...classes: (string | boolean | undefined)[]): string {
  return classes.filter(Boolean).join(' ');
}

function getVariantStyles(variant: Variant, danger: boolean): string {
  if (danger && variant === 'primary') {
    return 'bg-brand-900 text-white hover:bg-brand-900 hover:opacity-90 active:opacity-100 disabled:bg-ink-300 disabled:text-white';
  }

  switch (variant) {
    case 'primary':
      return 'bg-brand-800 text-white hover:bg-brand-800 hover:opacity-90 active:opacity-100 disabled:bg-ink-300 disabled:text-white';
    case 'secondary':
      return 'bg-fill-100 text-ink-900 hover:bg-fill-100 hover:opacity-70 active:opacity-100 disabled:bg-ink-300 disabled:text-ink-500';
    case 'ghost':
      return 'bg-transparent text-ink-900 hover:bg-fill-100 active:bg-fill-100 disabled:text-ink-300';
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
  const sizeClass = size === 'md' ? 'h-11 px-4 py-2.5' : 'h-9 px-3 py-2';
  const variantClass = getVariantStyles(variant, danger);
  const baseClass =
    'rounded-lg font-semibold text-xs transition-opacity duration-150 disabled:cursor-not-allowed focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-800';

  const finalClass = joinClasses(baseClass, sizeClass, variantClass, className);

  return (
    <button disabled={disabled} className={finalClass} {...props}>
      {children}
    </button>
  );
}
