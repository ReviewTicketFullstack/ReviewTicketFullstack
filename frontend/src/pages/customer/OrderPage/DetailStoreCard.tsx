import { Star } from "lucide-react";
import { Card } from "@/shared/ui";

export interface DetailStoreCardProps {
  storeName: string;
  rating: number;
  reviewCount: string;
  hasReviewEvent: boolean;
  onReviewClick?: () => void;
}

export function DetailStoreCard({
  storeName,
  rating,
  reviewCount,
  hasReviewEvent,
  onReviewClick,
}: DetailStoreCardProps) {
  return (
    <Card className="flex flex-col gap-4 p-5">
      {/* Chip Row — 리뷰이벤트 대상 메뉴가 하나라도 있는 가게만 표시 */}
      {hasReviewEvent && (
        <div>
          <span className="inline-block bg-red-700 text-white text-xs font-semibold px-3 py-1 rounded">
            리뷰 이벤트 적용 매장
          </span>
        </div>
      )}

      {/* Store Name Row */}
      <div>
        <h2 className="text-2xl font-bold">{storeName}</h2>
      </div>

      {/* Rating Row */}
      <div className="flex items-center gap-2">
        <Star size={16} className="fill-yellow-400 text-yellow-400" />
        <span className="text-base font-bold">{rating}</span>
        <button
          onClick={onReviewClick}
          className="text-sm text-gray-600 font-black cursor-pointer hover:text-gray-900 transition-colors"
        >
          리뷰 {reviewCount} {">"}
        </button>
      </div>
    </Card>
  );
}
