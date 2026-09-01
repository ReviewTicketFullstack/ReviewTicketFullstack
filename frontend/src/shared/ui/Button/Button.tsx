import type { ButtonHTMLAttributes } from "react";

export type ButtonVariant = "primary" | "secondary" | "ghost";
export type ButtonSize = "xlarge" | "large" | "medium" | "small";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;
}

/**
 * design.md Colors — hover 는 투명도가 아니라 한 단계 어두운 색으로 만든다.
 * disabled 도 opacity 를 낮추지 않고 Muted 계열 색을 쓴다. 라벨까지 흐려지면
 * 비활성인지 로딩인지 구분되지 않는다.
 */
const variantStyles: Record<ButtonVariant, string> = {
  primary:
    "bg-brand-800 text-white hover:bg-brand-950 active:bg-brand-950 disabled:bg-line-100 disabled:text-ink-500 disabled:cursor-not-allowed",
  secondary:
    "bg-fill-100 text-ink-900 hover:bg-line-100 active:bg-line-100 disabled:bg-fill-100 disabled:text-ink-300 disabled:cursor-not-allowed",
  ghost:
    "text-ink-700 hover:bg-fill-100 active:bg-line-100 disabled:text-ink-300 disabled:cursor-not-allowed",
};

/**
 * 라벨은 UI Label(font-semibold)로 본문 스케일과 분리한다. 굵기를 크기마다
 * 바꾸지 않는다. 높이는 터치 타깃 하한 44px(h-11)을 기본으로 둔다.
 */
const sizeStyles: Record<ButtonSize, string> = {
  xlarge: "h-14 px-5 text-base",
  large: "h-12 px-5 text-base",
  medium: "h-11 px-4 text-sm",
  small: "h-9 px-3 text-xs",
};

export function Button({
  children,
  variant = "primary",
  size = "large",
  fullWidth = false,
  className,
  ...props
}: ButtonProps) {
  const baseStyles =
    "inline-flex items-center justify-center gap-1 rounded-lg font-semibold " +
    "transition-[background-color,transform] duration-150 ease-out " +
    "active:scale-[0.98] disabled:active:scale-100 " +
    "motion-reduce:transition-none motion-reduce:active:scale-100";

  const widthStyle = fullWidth ? "w-full" : "";

  return (
    <button
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${widthStyle} ${className || ""}`}
      {...props}
    >
      {children}
    </button>
  );
}
