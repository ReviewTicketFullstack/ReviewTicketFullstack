import { useState } from "react";
import { Card } from "@/shared/ui";
import { ReviewButton } from "./ReviewButton";
import { ReviewModal } from "./ReviewModal";
import { formatOrderDate } from "@/entities/order/reviewTime";
import type { Order } from "@/entities/order";

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
            <span className="text-xs text-gray-500">
              {formatOrderDate(order.createdAt)}
            </span>
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
          // [TODO] 리뷰 등록 API 가 없어 화면 상태로만 표시한다.
          //        새로고침하면 서버가 모르는 상태라 다시 '작성 가능' 으로 돌아온다.
          setOrder({ ...order, reviewStatus: 'pending' });
        }}
      />
    </>
  );
}
