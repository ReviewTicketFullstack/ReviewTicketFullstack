import { useState } from 'react';
import { StatsBar } from './StatsBar';
import { ReviewTabs, type ReviewTab } from './ReviewTabs';
import { CompletedReviewItem } from './CompletedReviewItem';
import { PendingReviewItem } from './PendingReviewItem';
import { completedReviews, pendingOrders } from './mockData';

export function ReviewManagementPage() {
  const [activeTab, setActiveTab] = useState<ReviewTab>('completed');

  const totalCount = completedReviews.length;
  // 평균 별점 — 리뷰 0개일 때 0으로 나누는 걸 막기 위해 totalCount 체크 먼저
  const averageRating =
    totalCount === 0
      ? 0
      : completedReviews.reduce((sum, review) => sum + review.rating, 0) / totalCount;

  return (
    <div className="flex flex-col gap-4 p-6">
      <h1 className="text-xl font-bold text-ink-900">리뷰관리</h1>

      <StatsBar
        totalCount={totalCount}
        averageRating={averageRating}
        completedCount={completedReviews.length}
        pendingCount={pendingOrders.length}
      />

      <ReviewTabs active={activeTab} onChange={setActiveTab} />

      <div className="flex flex-col gap-3">
        {/* activeTab에 따라 리뷰완료/리뷰미작성 리스트를 다르게 렌더링 */}
        {activeTab === 'completed'
          ? completedReviews.map((review) => (
              <CompletedReviewItem key={review.id} review={review} />
            ))
          : pendingOrders.map((order) => (
              <PendingReviewItem key={order.id} order={order} />
            ))}
      </div>
    </div>
  );
}
