import { useEffect, useState } from 'react';
import { StatsBar } from './StatsBar';
import { ReviewTabs, type ReviewTab } from './ReviewTabs';
import { CompletedReviewItem } from './CompletedReviewItem';
import { PendingReviewItem } from './PendingReviewItem';
import {
  getMyPendingOrders,
  getMyStoreReviews,
  type OwnerReview,
  type PendingOrder,
} from '@/api/reviewApi';

export function ReviewManagementPage() {
  const [activeTab, setActiveTab] = useState<ReviewTab>('completed');
  const [completedReviews, setCompletedReviews] = useState<OwnerReview[]>([]);
  const [pendingOrders, setPendingOrders] = useState<PendingOrder[]>([]);
  // 총 리뷰 수와 평균 별점은 서버가 가게 표에서 그대로 준다. 화면이 배열을
  // 훑어 직접 세면 목록에 페이지 나누기가 붙는 순간 보이는 몇 건만으로 계산해 틀어진다.
  const [totalCount, setTotalCount] = useState(0);
  const [averageRating, setAverageRating] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    Promise.all([
      getMyStoreReviews(controller.signal),
      getMyPendingOrders(controller.signal),
    ])
      .then(([reviewList, pending]) => {
        setCompletedReviews(reviewList.reviews);
        setTotalCount(reviewList.reviewNumber);
        setAverageRating(reviewList.reviewValue);
        setPendingOrders(pending);
      })
      .catch((err) => {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setError('리뷰를 불러오지 못했습니다.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, []);

  // 서버는 둘을 한 목록에 담아 보낸다(GET /api/stores/me/orders/pending).
  // 마감 전이면 작성 대기, 마감이 지났는데도 리뷰가 없으면 미이행.
  const pendingList = pendingOrders.filter((o) => o.reviewStatus === 'pending');
  const expiredList = pendingOrders.filter((o) => o.reviewStatus === 'expired');

  return (
    <div className="flex flex-col gap-4 p-6">
      <h1 className="text-xl font-bold text-ink-900">리뷰관리</h1>

      <StatsBar
        totalCount={totalCount}
        averageRating={averageRating}
        completedCount={completedReviews.length}
        pendingCount={pendingList.length}
        expiredCount={expiredList.length}
      />

      <ReviewTabs active={activeTab} onChange={setActiveTab} />

      {isLoading && <p className="text-sm text-neutral-500">불러오는 중...</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex flex-col gap-3">
        {/* activeTab에 따라 리뷰완료/작성 대기/미이행 리스트를 다르게 렌더링 */}
        {activeTab === 'completed'
          ? completedReviews.map((review) => (
              <CompletedReviewItem key={review.reviewId} review={review} />
            ))
          : (activeTab === 'pending' ? pendingList : expiredList).map((order) => (
              <PendingReviewItem key={order.orderId} order={order} />
            ))}
      </div>
    </div>
  );
}
