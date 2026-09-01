import { Star, UtensilsCrossed } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Badge, Card } from "@/shared/ui";

export interface StoreCardProps {
  storeId: number;
  storeName: string;
  rating: number;
  reviewCount: string;
  imageUrl: string | null;
  // 리뷰 작성 가능 메뉴가 하나라도 있을때 true - 리뷰 뱃지 노출 조건
  hasReviewEvent: boolean;
}

export function StoreCard({
  storeId,
  storeName,
  rating,
  reviewCount,
  imageUrl,
  hasReviewEvent,
}: StoreCardProps) {
  const navigate = useNavigate();

  return (
    <Card
      interactive
      className="flex gap-3 p-3"
      onClick={() => navigate(`/order/${storeId}`)}
    >
      {/* 미디어는 정사각 고정. 이미지가 없으면 fill-100 배경 + 아이콘 */}
      <div className="flex aspect-square w-24 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-fill-100">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt=""
            className="h-full w-full object-cover"
          />
        ) : (
          <UtensilsCrossed size={24} className="text-ink-300" aria-hidden="true" />
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col justify-center gap-2">
        <div className="flex items-center gap-2">
          <h3 className="min-w-0 truncate text-base font-bold text-ink-900">
            {storeName}
          </h3>
          {hasReviewEvent && (
            <Badge variant="accent" className="shrink-0">
              리뷰
            </Badge>
          )}
        </div>

        <div className="flex items-center gap-1">
          <Star size={14} className="fill-star text-star" aria-hidden="true" />
          <span className="text-sm font-semibold text-ink-900">{rating}</span>
          <span className="text-xs text-ink-500">리뷰 {reviewCount}</span>
        </div>
      </div>
    </Card>
  );
}
