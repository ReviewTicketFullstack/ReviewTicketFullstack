import type { HTMLAttributes } from 'react';

export type BadgeProps = HTMLAttributes<HTMLSpanElement>;

export function Badge({ children, ...props }: BadgeProps) {
  return <span {...props}>{children}</span>;
}
