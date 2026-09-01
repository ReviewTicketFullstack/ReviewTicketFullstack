export interface LottieSlotProps {
  /** 스크린리더가 읽을 대체 문구. 장식이면 비워 둔다 */
  label?: string;
  className?: string;
}

/**
 * Lottie 애니메이션이 들어갈 자리.
 *
 * 지금은 자리만 잡아 두는 플레이스홀더다. 실제 애니메이션을 붙일 때는 이
 * 컴포넌트를 통째로 갈아끼우면 된다 — FeatureCard 의 media 슬롯이 이걸
 * 기본값으로 쓰므로, 호출부를 고칠 필요 없이 여기 한 곳만 바꾸면 된다.
 *
 *   1. lottie 재생 라이브러리를 추가한다(새 의존성이라 사전 합의 필요).
 *   2. 아래 <span> 자리에 플레이어를 렌더한다.
 *   3. 바깥 치수(aspect-square, 배경, 라운드)는 그대로 두면 레이아웃이 안 흔들린다.
 */
export function LottieSlot({ label = "", className = "" }: LottieSlotProps) {
  return (
    <div
      role={label ? "img" : "presentation"}
      aria-label={label || undefined}
      className={`flex aspect-square w-full items-center justify-center rounded-xl bg-brand-50 ${className}`}
    >
      {/* 플레이스홀더 — 실제 Lottie 로 교체될 자리 */}
      <span aria-hidden="true" className="text-4xl">
        🎫
      </span>
    </div>
  );
}
