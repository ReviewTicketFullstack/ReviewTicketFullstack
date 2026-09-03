import { ChevronRight, Star } from "lucide-react";
import { Badge, Card } from "@/shared/ui";

export interface DetailStoreCardProps {
  storeName: string;
  rating: number;
  reviewCount: string;
  hasReviewEvent: boolean;
  /** 리뷰 개수를 눌렀을 때 — 리뷰 화면으로 이동 */
  onReviewClick: () => void;
}

export function DetailStoreCard({
  storeName,
  rating,
  reviewCount,
  hasReviewEvent,
  onReviewClick,
}: DetailStoreCardProps) {
  return (
    <Card className="flex flex-col items-start gap-3 p-5">
      {/* Chip Row — 리뷰이벤트 대상 메뉴가 하나라도 있는 가게만 표시 */}
      {hasReviewEvent && <Badge variant="accent">리뷰 이벤트 매장</Badge>}

      <h2 className="text-xl font-bold text-ink-900">{storeName}</h2>

      {/* Rating Row */}
      <div className="flex items-center gap-2">
        <span className="flex items-center gap-1">
          <Star size={16} className="fill-star text-star" aria-hidden="true" />
          <span className="text-sm font-bold text-ink-900">{rating}</span>
        </span>

        <button
          type="button"
          onClick={onReviewClick}
          className="-mx-1 flex items-center gap-0.5 rounded-lg px-1 py-1 text-xs font-semibold text-ink-700 transition-colors hover:bg-fill-100 active:bg-line-100"
        >
          리뷰 {reviewCount}
          <ChevronRight size={14} aria-hidden="true" />
        </button>
      </div>
    </Card>
  );
}
