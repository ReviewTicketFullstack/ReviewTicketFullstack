interface StatsBarProps {
  totalCount: number;
  averageRating: number;
  completedCount: number;
  pendingCount: number;
}

export function StatsBar({
  totalCount,
  averageRating,
  completedCount,
  pendingCount,
}: StatsBarProps) {
  return (
    <div className="flex gap-3">
      <div className="flex flex-1 items-center justify-center gap-6 rounded-lg bg-neutral-100 p-4">
        <span className="flex items-center gap-2">
          <span className="size-2.5 rounded-full bg-green-600" />
          리뷰완료 {completedCount}
        </span>
        <span className="flex items-center gap-2">
          <span className="size-2.5 rounded-full bg-red-600" />
          리뷰미작성 {pendingCount}
        </span>
      </div>
      <div className="flex flex-1 items-center justify-center rounded-lg bg-neutral-100 p-4">
        총리뷰수 {totalCount}, 전체별점 {averageRating.toFixed(1)}
      </div>
    </div>
  );
}
