import { useState } from "react";
import { Card } from "@/shared/ui";
import { ReviewButton } from "./ReviewButton";
import { ReviewModal } from "./ReviewModal";
import { updateOrderReviewStatus } from "@/features/order/orderStorage";
import type { Order } from "@/shared/types/order";

export interface OrderHistoryItemProps {
  order: Order;
}

export function OrderHistoryItem({ order: initialOrder }: OrderHistoryItemProps) {
  const [order, setOrder] = useState<Order>(initialOrder);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

  return (
    <>
      <Card className="p-5">
        <div className="space-y-3">
          {/* Store Name & Review Badge */}
          <div className="flex flex-row items-center gap-2">
            <h3 className="text-lg font-bold">{order.storeName}</h3>

            {order.hasReviewBadge && (
              <span className="rounded bg-red-700 px-2 py-0.5 text-xs font-semibold text-white">
                리뷰
              </span>
            )}
          </div>

          {/* Menu Info */}
          <div className="flex items-center justify-between">
            <span className="text-base text-gray-700">{order.menuName}</span>
            <span className="text-base font-bold">
              {order.price.toLocaleString("ko-KR")}원
            </span>
          </div>

          {/* Date */}
          <div className="flex items-center justify-between pt-2 border-t border-gray-200">
            <span className="text-xs text-gray-500">{order.createdAt}</span>
          </div>

          {/* Review Button */}
          <div className="pt-3">
            <ReviewButton
              reviewDeadline={order.reviewDeadline}
              hasReviewBadge={order.hasReviewBadge}
              reviewStatus={order.reviewStatus}
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
        onSubmitSuccess={() => {
          const updatedOrder = { ...order, reviewStatus: 'pending' as const };
          setOrder(updatedOrder);
          updateOrderReviewStatus(order.id, 'pending');
        }}
      />
    </>
  );
}
