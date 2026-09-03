import type { ButtonHTMLAttributes } from "react";

export type ButtonVariant = "primary" | "secondary" | "ghost";
export type ButtonSize = "xlarge" | "large" | "medium" | "small";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;
}

const variantStyles = {
  primary:
    "bg-red-700 text-white hover:bg-red-800 active:bg-red-900 disabled:bg-gray-400 disabled:cursor-not-allowed",
  secondary:
    "bg-gray-200 text-gray-900 hover:bg-gray-300 active:bg-gray-400 disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed",
  ghost:
    "text-gray-900 hover:bg-gray-100 active:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed",
};

const sizeStyles = {
  xlarge: "px-5 py-4 text-base font-semibold rounded-lg h-14",
  large: "px-4 py-3 text-base font-semibold rounded-lg h-12",
  medium: "px-4 py-2 text-sm font-semibold rounded-lg h-10",
  small: "px-3 py-2 text-xs font-semibold rounded-lg h-9",
};

export function Button({
  children,
  variant = "primary",
  size = "large",
  fullWidth = false,
  className,
  ...props
}: ButtonProps) {
  const baseStyles = "font-semibold rounded-lg transition-colors";
  const variantStyle = variantStyles[variant];
  const sizeStyle = sizeStyles[size];
  const widthStyle = fullWidth ? "w-full" : "";

  return (
    <button
      className={`${baseStyles} ${variantStyle} ${sizeStyle} ${widthStyle} ${className || ""}`}
      {...props}
    >
      {children}
    </button>
  );
}
