import { Star } from "lucide-react";
import { Card } from "@/shared/ui";

export interface DetailStoreCardProps {
  storeName: string;
  rating: number;
  reviewCount: string;
}

export function DetailStoreCard({
  storeName,
  rating,
  reviewCount,
}: DetailStoreCardProps) {
  return (
    <Card className="flex flex-col gap-4 p-5">
      {/* Chip Row */}
      <div>
        <span className="inline-block bg-red-700 text-white text-xs font-semibold px-3 py-1 rounded">
          sample리뷰 이벤트 적용 매장
        </span>
      </div>

      {/* Store Name Row */}
      <div>
        <h2 className="text-2xl font-bold">{storeName}</h2>
      </div>

      {/* Rating Row */}
      <div className="flex items-center gap-2">
        <Star size={16} className="fill-yellow-400 text-yellow-400" />
        <span className="text-base font-bold">{rating}</span>
        <span className="text-sm text-gray-600">
          리뷰 {reviewCount}
          {">"}
        </span>
      </div>
    </Card>
  );
}
