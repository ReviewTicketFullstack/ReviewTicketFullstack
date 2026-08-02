import type { HTMLAttributes } from 'react';

type Variant = 'accent' | 'info';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: Variant;
}

function getVariantStyles(variant: Variant): string {
  switch (variant) {
    case 'accent':
      return 'bg-brand-800 text-white';
    case 'info':
      return 'bg-fill-100 text-ink-900';
  }
}

export function Badge({ variant = 'info', className = '', children, ...props }: BadgeProps) {
  const variantClass = getVariantStyles(variant);
  const baseClass = 'rounded-sm px-1.5 py-0.5 text-xs font-semibold';

  return (
    <span className={`${baseClass} ${variantClass} ${className}`} {...props}>
      {children}
    </span>
  );
}
