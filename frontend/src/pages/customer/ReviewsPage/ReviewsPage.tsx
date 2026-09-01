import { useEffect, useState } from "react";
import { Card, EmptyState, Loading, StarRating } from "@/shared/ui";
import { getMyOrders } from "@/api/orderApi";
import { getStoreReviews, type PublicReview } from "@/api/reviewApi";
import { formatOrderDate } from "@/entities/order/reviewTime";

interface ReviewWithStore extends PublicReview {
  storeName: string;
}

export function ReviewsPage() {
  const [reviews, setReviews] = useState<ReviewWithStore[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    const fetchReviews = async () => {
      try {
        const orders = await getMyOrders(controller.signal);

        // 리뷰를 작성한 주문들만 필터링
        const reviewedOrders = orders.filter((order) => order.reviewStatus === "done");

        if (reviewedOrders.length === 0) {
          setReviews([]);
          setIsLoading(false);
          return;
        }

        // 각 가게의 리뷰를 가져오고, 본인의 리뷰만 필터링
        const allReviews: ReviewWithStore[] = [];
        const storeIds = Array.from(new Set(reviewedOrders.map((o) => o.storeId)));

        for (const storeId of storeIds) {
          try {
            const storeReviews = await getStoreReviews(storeId, controller.signal);
            const storeName = reviewedOrders.find((o) => o.storeId === storeId)?.storeName || "";

            // 모든 리뷰에 가게 이름을 추가
            storeReviews.forEach((review) => {
              allReviews.push({
                ...review,
                storeName,
              });
            });
          } catch (e: unknown) {
            if (e instanceof DOMException && e.name === "AbortError") return;
          }
        }

        // 최신순으로 정렬
        allReviews.sort(
          (a, b) =>
            new Date(b.reviewCreatedAt).getTime() - new Date(a.reviewCreatedAt).getTime()
        );

        setReviews(allReviews);
      } catch (e: unknown) {
        if (e instanceof DOMException && e.name === "AbortError") return;
        setError("리뷰를 불러오지 못했습니다.");
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    };

    fetchReviews();

    return () => controller.abort();
  }, []);

  return (
    <div className="flex flex-col gap-8 px-5 py-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-xl font-bold text-ink-900">리뷰</h1>
        <p className="text-sm text-ink-700">작성한 리뷰를 확인할 수 있어요.</p>
      </div>

      {isLoading && <Loading />}

      {error && (
        <p className="rounded-lg bg-brand-50 px-3 py-3 text-sm text-brand-900">
          {error}
        </p>
      )}

      {!isLoading && !error && reviews.length === 0 && (
        <EmptyState
          icon="✍️"
          message="아직 작성한 리뷰가 없어요. 주문내역에서 리뷰를 남기면 여기에 모여요."
        />
      )}

      {reviews.length > 0 && (
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
                      {review.storeName}
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
      )}
    </div>
  );
}
