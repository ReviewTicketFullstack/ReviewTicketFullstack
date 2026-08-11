import { useEffect, useState } from "react";
import { Card } from "@/shared/ui";
import { getStoreReviews, type PublicReview } from "@/api/reviewApi";
import { formatOrderDate } from "@/entities/order/reviewTime";

/**
 * 가게 상세 아래에 붙는 리뷰 목록.
 *
 * 그 가게에 달린 리뷰를 전부 보여준다 — 누가 썼는지 가리지 않는다. 서버가
 * 최신순으로 정렬해 주므로 여기서 다시 정렬하지 않는다.
 *
 * 판정 근거(유사도, 대조한 표본 사진)는 응답에 실리지 않는다. 모르는 손님도
 * 보는 화면이라 사장에게만 내려주는 값이다.
 */
export function StoreReviewSection({ storeId }: { storeId: number }) {
  const [reviews, setReviews] = useState<PublicReview[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    getStoreReviews(storeId, controller.signal)
      .then(setReviews)
      .catch((e: unknown) => {
        if (e instanceof DOMException && e.name === "AbortError") return;
        setError("리뷰를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, [storeId]);

  return (
    <div className="space-y-3 px-5">
      <h2 className="text-lg font-bold">
        리뷰
        {reviews.length > 0 && (
          <span className="ml-1 text-gray-500">({reviews.length})</span>
        )}
      </h2>

      {isLoading && <p className="text-sm text-gray-500">불러오는 중...</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}

      {!isLoading && !error && reviews.length === 0 && (
        <p className="py-6 text-center text-sm text-gray-500">
          아직 작성된 리뷰가 없어요.
        </p>
      )}

      {reviews.map((review) => (
        <Card key={review.reviewId} className="p-4">
          <div className="flex gap-3">
            <img
              src={review.reviewImageUrl}
              alt={`${review.menuName} 리뷰 사진`}
              className="size-20 flex-shrink-0 rounded-lg bg-gray-200 object-cover"
            />

            <div className="flex flex-1 flex-col gap-1">
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold">{review.displayName}</span>
                <span className="text-xs text-gray-500">
                  {formatOrderDate(review.reviewCreatedAt)}
                </span>
              </div>

              <div className="flex items-center gap-2">
                {/* 별점은 받은 수만큼 노란 별, 나머지는 회색으로 다섯 칸을 채운다 */}
                <span className="text-sm" aria-label={`별점 ${review.reviewRating}점`}>
                  <span className="text-yellow-400">
                    {"★".repeat(review.reviewRating)}
                  </span>
                  <span className="text-gray-300">
                    {"★".repeat(5 - review.reviewRating)}
                  </span>
                </span>
                <span className="text-xs text-gray-500">{review.menuName}</span>
              </div>

              <p className="text-sm text-gray-700">{review.reviewContent}</p>
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}
