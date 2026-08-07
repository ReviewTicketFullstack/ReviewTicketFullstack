import { Star } from "lucide-react";
import { Card } from "@/shared/ui";
import { useNavigate } from "react-router-dom";

export interface StoreCardProps {
  storeId: number;
  storeName: string;
  rating: number;
  reviewCount: string;
  imageUrl?: string;
}

export function StoreCard({
  storeId,
  storeName,
  rating,
  reviewCount,
  imageUrl,
}: StoreCardProps) {
  const navigate = useNavigate();

  return (
    <Card
      className="
    flex cursor-pointer gap-4 p-4
    transition-all duration-200
    hover:shadow-xl
    hover:bg-gray-100
    active:scale-[0.98]
    "
      onClick={() => navigate(`/order/${storeId}`)}
    >
      {/* Image Section (1 part) */}
      <div className="flex-shrink-0">
        <div className="aspect-square w-24 rounded-lg bg-gray-200 flex items-center justify-center">
          {imageUrl ? (
            <img
              src={imageUrl}
              alt={storeName}
              className="h-full w-full object-cover rounded-lg"
            />
          ) : (
            <span className="text-gray-400 text-sm">Image</span>
          )}
        </div>
      </div>

      {/* Content Section (3 parts) */}
      <div className="flex-1 flex flex-col justify-center gap-2">
        {/* First Row: Store Name + Review Chip */}
        <div className="flex items-center gap-2">
          <h3 className="text-lg font-bold">{storeName}</h3>
          <div className="bg-red-700 text-white px-2 py-1 rounded text-xs font-semibold">
            리뷰
          </div>
        </div>

        {/* Second Row: Rating Info */}
        <div className="flex items-center gap-1">
          <Star size={16} className="fill-yellow-400 text-yellow-400" />
          <span className="text-sm font-bold">{rating}</span>
          <span className="text-sm text-gray-600">({reviewCount})</span>
        </div>
      </div>
    </Card>
  );
}
