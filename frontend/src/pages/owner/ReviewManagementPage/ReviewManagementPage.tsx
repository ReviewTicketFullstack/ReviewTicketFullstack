import { useState } from 'react';
import { StatsBar } from './StatsBar';
import { ReviewTabs, type ReviewTab } from './ReviewTabs';
import { CompletedReviewItem } from './CompletedReviewItem';
import { PendingReviewItem } from './PendingReviewItem';
import { completedReviews, pendingOrders } from './mockData';

export function ReviewManagementPage() {
  const [activeTab, setActiveTab] = useState<ReviewTab>('completed');

  const totalCount = completedReviews.length;
  const averageRating =
    totalCount === 0
      ? 0
      : completedReviews.reduce((sum, review) => sum + review.rating, 0) / totalCount;

  return (
    <div className="flex flex-col gap-4 p-6">
      <StatsBar
        totalCount={totalCount}
        averageRating={averageRating}
        completedCount={completedReviews.length}
        pendingCount={pendingOrders.length}
      />

      <ReviewTabs active={activeTab} onChange={setActiveTab} />

      <div className="flex flex-col gap-3">
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
