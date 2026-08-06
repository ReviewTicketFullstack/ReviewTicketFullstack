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
    /*총 리뷰수, 전체별점, 리뷰완료/리뷰미작성/미이행 수치 표시*/
    <div className="flex items-center justify-between rounded-lg bg-neutral-100 p-4">
      <div className="flex items-center gap-6">
        <span className="flex items-center gap-2">
          <span className="size-2.5 rounded-full bg-green-600" />
          리뷰완료 {completedCount}
        </span>
        <span className="flex items-center gap-2">
          <span className="size-2.5 rounded-full bg-red-600" />
          리뷰미작성 {pendingCount}
        </span>
        {/* FE-2.5 값 계산 로직 미확정, 자리만 잡아둔 고정값 */}
        <span className="flex items-center gap-2">
          <span className="size-2.5 rounded-full bg-orange-500" />
          미이행 0
        </span>
      </div>
      {/*우측 상단 총리뷰수, 전체별점(별 아이콘+평균) 표시 */}
      <span className="flex items-center gap-1">
        총리뷰수 {totalCount}, 전체별점
        <img src="/star.svg" alt="" className="size-3.5" />
        {averageRating.toFixed(1)}
      </span>
    </div>
  );
}
