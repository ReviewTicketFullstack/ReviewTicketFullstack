import type { ReactNode } from "react";
import { Card } from "../Card";
import { LottieSlot } from "./LottieSlot";

export interface FeatureCardProps {
  /** 제목 위 한 줄. 배지처럼 작게 뜬다 */
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  /** 하단 액션. 버튼이나 링크를 그대로 넣는다 */
  action?: ReactNode;
  /**
   * 왼쪽 시각 요소가 들어갈 자리. 넘기지 않으면 Lottie 플레이스홀더가 뜬다.
   * 나중에 실제 애니메이션을 붙일 때 이 자리에 플레이어를 넘기거나,
   * LottieSlot 내부만 바꾸면 된다.
   */
  media?: ReactNode;
  /** media 영역의 폭. 카드가 좁은 자리에 들어갈 때 줄인다 */
  mediaClassName?: string;
  className?: string;
}

/**
 * Greeting Card 와 같은 조형을 쓰는 홍보·안내 카드.
 *
 * 문구와 시각 요소만 갈아끼우면 다른 자리에도 그대로 쓸 수 있게 열어 뒀다.
 * 색·간격·라운드는 design.md 스케일에서만 고른다 — 이 컴포넌트에 hex 는 없다.
 */
export function FeatureCard({
  eyebrow,
  title,
  description,
  action,
  media,
  mediaClassName = "w-20 shrink-0",
  className = "",
}: FeatureCardProps) {
  return (
    <Card className={`flex items-center gap-5 p-5 ${className}`}>
      <div className={mediaClassName}>{media ?? <LottieSlot />}</div>

      <div className="flex min-w-0 flex-1 flex-col gap-2">
        {eyebrow && (
          <span className="text-xs font-semibold text-brand-800">{eyebrow}</span>
        )}

        <p className="text-base font-bold text-ink-900">{title}</p>

        {description && (
          <p className="text-sm leading-relaxed text-ink-700">{description}</p>
        )}

        {action}
      </div>
    </Card>
  );
}
