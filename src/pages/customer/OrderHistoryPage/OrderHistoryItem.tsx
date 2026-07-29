import { useState } from 'react';
import { Card } from '@/shared/ui';
import { ReviewButton } from './ReviewButton';
import { ReviewModal } from './ReviewModal';
import type { Order } from '@/shared/types/order';

export interface OrderHistoryItemProps {
  order: Order;
}

export function OrderHistoryItem({ order }: OrderHistoryItemProps) {
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

  return (
    <>
      <Card className="p-5">
        <div className="space-y-3">
          {/* Store Name */}
          <h3 className="text-lg font-bold">{order.storeName}</h3>

          {/* Menu Info */}
          <div className="flex items-center justify-between">
            <span className="text-base text-gray-700">{order.menuName}</span>
            <span className="text-base font-bold">
              {order.price.toLocaleString('ko-KR')}
            </span>
          </div>

          {/* Review Badge & Date */}
          <div className="flex items-center justify-between pt-2 border-t border-gray-200">
            <div>
              {order.hasReviewBadge && (
                <span className="inline-block bg-red-700 text-white text-xs font-semibold px-2 py-0.5 rounded">
                  리뷰
                </span>
              )}
            </div>
            <span className="text-xs text-gray-500">{order.createdAt}</span>
          </div>

          {/* Review Button */}
          <div className="pt-3">
            <ReviewButton
              reviewDeadline={order.reviewDeadline}
              hasReviewBadge={order.hasReviewBadge}
              onReviewClick={() => setIsReviewModalOpen(true)}
            />
          </div>
        </div>
      </Card>

      <ReviewModal
        open={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        storeName={order.storeName}
        menuName={order.menuName}
      />
    </>
  );
}
