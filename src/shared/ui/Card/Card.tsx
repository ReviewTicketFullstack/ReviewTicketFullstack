import type { HTMLAttributes } from 'react';

export type CardProps = HTMLAttributes<HTMLDivElement>;

export function Card({ children, className, ...props }: CardProps) {
  return (
    <div
      className={`rounded-2xl bg-white shadow-[0_20px_50px_rgba(0,0,0,0.5)] ${className || ''}`}
      {...props}
    >
      {children}
    </div>
  );
}
