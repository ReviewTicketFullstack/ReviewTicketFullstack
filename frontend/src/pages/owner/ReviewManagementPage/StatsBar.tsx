import { Star } from "lucide-react";

interface StatsBarProps {
  totalCount: number;
  averageRating: number;
  completedCount: number;
  pendingCount: number;
  expiredCount: number;
}

/**
 * 상태 점의 색은 보조 단서다 — 라벨을 항상 함께 둬서 색만으로 구분되지 않게 한다.
 * 색은 Success(green-700) / Danger(brand-900) / 주의(star) 역할에서만 고른다.
 */
const DOT_COLORS = {
  completed: "bg-green-700",
  expired: "bg-brand-900",
  pending: "bg-star",
} as const;

function Stat({
  tone,
  label,
  value,
}: {
  tone: keyof typeof DOT_COLORS;
  label: string;
  value: number;
}) {
  return (
    <span className="flex items-center gap-2 text-sm text-ink-700">
      <span
        aria-hidden="true"
        className={`size-2 shrink-0 rounded-full ${DOT_COLORS[tone]}`}
      />
      {label}
      <span className="font-bold text-ink-900">{value}</span>
    </span>
  );
}

export function StatsBar({
  totalCount,
  averageRating,
  completedCount,
  pendingCount,
  expiredCount,
}: StatsBarProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-surface p-5 shadow-flat">
      <div className="flex flex-wrap items-center gap-5">
        <Stat tone="completed" label="리뷰완료" value={completedCount} />
        <Stat tone="expired" label="미이행" value={expiredCount} />
        <Stat tone="pending" label="작성 대기" value={pendingCount} />
      </div>

      <div className="flex items-center gap-3 text-sm text-ink-700">
        <span>
          총 리뷰 <span className="font-bold text-ink-900">{totalCount}</span>
        </span>
        <span className="flex items-center gap-1">
          <Star size={14} className="fill-star text-star" aria-hidden="true" />
          <span className="font-bold text-ink-900">
            {averageRating.toFixed(1)}
          </span>
        </span>
      </div>
    </div>
  );
}
