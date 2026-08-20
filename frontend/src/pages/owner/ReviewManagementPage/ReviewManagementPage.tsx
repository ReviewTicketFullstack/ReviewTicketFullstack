import { useCallback, useEffect, useRef, useState } from 'react';
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
  // 값이 바뀌면 아래 useEffect 가 다시 돈다. 새로고침 버튼과 카드 만료가 올린다.
  const [reloadKey, setReloadKey] = useState(0);

  // 카드에 넘기는 값이라 참조가 고정돼야 한다. 인라인 화살표로 넘기면 매 렌더마다
  // 새 함수가 돼 카드의 카운트다운 interval 이 계속 버려지고 다시 만들어진다.
  //
  // 여러 카드가 비슷한 시점에 동시 만료되면 onExpire 가 카드 수만큼 연달아 호출된다.
  // debounce 로 묶어서 1초 안에 몰린 호출은 재조회 1번으로 합친다.
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reload = useCallback(() => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setReloadKey((k) => k + 1), 1000);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setIsLoading(true);
    setError(null);

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
  }, [reloadKey]);

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

      <ReviewTabs
        active={activeTab}
        onChange={setActiveTab}
        onRefresh={reload}
        isRefreshing={isLoading}
      />

      {isLoading && <p className="text-sm text-neutral-500">불러오는 중...</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex flex-col gap-3">
        {/* activeTab에 따라 리뷰완료/작성 대기/미이행 리스트를 다르게 렌더링 */}
        {activeTab === 'completed'
          ? completedReviews.map((review) => (
              <CompletedReviewItem key={review.reviewId} review={review} />
            ))
          : (activeTab === 'pending' ? pendingList : expiredList).map((order) => (
              <PendingReviewItem
                key={order.orderId}
                order={order}
                onExpire={reload}
              />
            ))}
      </div>
    </div>
  );
}
