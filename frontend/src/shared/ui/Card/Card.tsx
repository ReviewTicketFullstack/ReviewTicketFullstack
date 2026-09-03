import type { HTMLAttributes } from "react";

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  interactive?: boolean;
  bordered?: boolean;
}

export function Card({
  children,
  className,
  interactive = false,
  bordered = false,
  ...props
}: CardProps) {
  return (
    <div
      className={`
        rounded-2xl
        bg-white
        shadow-[0_20px_50px_rgba(0,0,0,0.5)]
        ${bordered ? "border border-gray-200" : ""}
        ${interactive ? "cursor-pointer transition hover:bg-slate-50 active:scale-[0.98]" : ""}
        ${className || ""}
        `}
      {...props}
    >
      {children}
    </div>
  );
}
