export interface CountBadgeProps {
  count: number;
  className?: string;
}

/**
 * 아이콘 위에 얹는 카운트 배지.
 * 두 자리를 넘으면 99+로 절삭하고, Surface 색 링으로 배경과 분리한다.
 * 0 이하면 렌더하지 않는다(호출부에서 조건 분기를 반복하지 않게).
 */
export function CountBadge({ count, className = '' }: CountBadgeProps) {
  if (count <= 0) return null;

  return (
    <span
      className={`flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-800 px-1 text-xs font-semibold text-white ring-2 ring-surface ${className}`}
    >
      {count > 99 ? '99+' : count}
    </span>
  );
}
