// 비밀번호 조건 오류 메시지
// 비밀번호 불일치 메시지
// 이메일 형식 안내 메시지

import { type ReactNode } from "react";

export interface InputHelperTextProps {
  children: ReactNode;
  variant?: "error" | "info";
  id?: string;
  visible?: boolean;
}

export function InputHelperText({
  children,
  variant = "info",
  id,
}: InputHelperTextProps) {
  const variantClass = variant === "error" ? "text-brand-900" : "text-ink-500";

  return (
    <p
      id={id}
      className={`text-xs ${variantClass}`}
      style={{ animation: "fadeInSlideDown 0.2s ease-out" }}
    >
      {children}
    </p>
  );
}
