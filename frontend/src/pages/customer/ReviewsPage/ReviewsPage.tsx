import { useEffect, useState } from "react";
import { Card } from "@/shared/ui";
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
    <div className="space-y-4 px-5 py-6">
      <div>
        <h1 className="text-xl font-bold">리뷰</h1>
        <p className="text-sm text-gray-600">작성한 리뷰를 확인할 수 있습니다.</p>
      </div>

      {isLoading && <p className="text-center text-sm text-gray-500 py-12">불러오는 중...</p>}

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>
      )}

      {!isLoading && !error && reviews.length === 0 && (
        <div className="text-center py-12">
          <p className="text-sm text-gray-500">작성한 리뷰가 없습니다.</p>
        </div>
      )}

      {reviews.length > 0 && (
        <div className="space-y-3">
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
                    <span className="text-xs text-gray-500 font-semibold">
                      {review.storeName}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
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

                  <span className="text-xs text-gray-500 pt-1">
                    {formatOrderDate(review.reviewCreatedAt)}
                  </span>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
