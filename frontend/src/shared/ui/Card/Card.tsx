import type { HTMLAttributes } from "react";

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** 카드 전체가 하나의 터치 타깃일 때. shadow-raised 로 올리고 누름 반응을 붙인다 */
  interactive?: boolean;
  /** 그림자 대신 Border 1px 로 배경과 나눈다. 클릭 불가한 정보 카드용 */
  bordered?: boolean;
}

export function Card({
  children,
  className,
  interactive = false,
  bordered = false,
  ...props
}: CardProps) {
  // design.md Elevation — 정적 카드는 shadow-flat, 통째로 눌리는 카드만
  // shadow-raised 로 한 단계 올린다. bordered 는 그림자 없이 선으로만 나눈다.
  const elevation = bordered
    ? "border border-line-100"
    : interactive
      ? "shadow-raised"
      : "shadow-flat";

  const interactions = interactive
    ? "cursor-pointer transition-[transform,box-shadow,background-color] duration-200 ease-out hover:bg-surface-sub active:scale-[0.99] motion-reduce:transition-none motion-reduce:active:scale-100"
    : "";

  return (
    <div
      className={`rounded-2xl bg-surface ${elevation} ${interactions} ${className || ""}`}
      {...props}
    >
      {children}
    </div>
  );
}
