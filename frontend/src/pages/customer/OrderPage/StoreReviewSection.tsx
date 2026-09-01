import { useEffect, useState } from "react";
import { Card, EmptyState, Loading, StarRating } from "@/shared/ui";
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
    <div className="flex flex-col gap-3 px-5">
      <h2 className="text-base font-bold text-ink-900">
        리뷰
        {reviews.length > 0 && (
          <span className="ml-1 text-ink-500">({reviews.length})</span>
        )}
      </h2>

      {isLoading && <Loading />}
      {error && <p className="text-sm text-brand-900">{error}</p>}

      {!isLoading && !error && reviews.length === 0 && (
        <EmptyState
          icon="💬"
          message="아직 작성된 리뷰가 없어요. 첫 리뷰를 남겨보세요."
        />
      )}

      <ul className="flex flex-col gap-3">
        {reviews.map((review) => (
          <li key={review.reviewId}>
            <Card className="flex gap-3 p-3">
              <img
                src={review.reviewImageUrl}
                alt={`${review.menuName} 리뷰 사진`}
                className="size-20 shrink-0 rounded-xl bg-fill-100 object-cover"
              />

              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <div className="flex items-center gap-2">
                  <span className="min-w-0 truncate text-sm font-bold text-ink-900">
                    {review.displayName}
                  </span>
                  <span className="ml-auto shrink-0 text-xs text-ink-500">
                    {formatOrderDate(review.reviewCreatedAt)}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <StarRating rating={review.reviewRating} />
                  <span className="min-w-0 truncate text-xs text-ink-500">
                    {review.menuName}
                  </span>
                </div>

                <p className="text-sm leading-relaxed text-ink-700">
                  {review.reviewContent}
                </p>
              </div>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
