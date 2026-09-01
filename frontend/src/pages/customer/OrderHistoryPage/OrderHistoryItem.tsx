import { useState } from "react";
import { Badge, Card } from "@/shared/ui";
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
      <Card className="flex flex-col gap-3 p-5">
        {/* Store Name & Review Badge */}
        <div className="flex items-center gap-2">
          <h3 className="min-w-0 truncate text-base font-bold text-ink-900">
            {order.storeName}
          </h3>

          {order.hasReviewBadge && (
            <Badge variant="accent" className="shrink-0">
              리뷰
            </Badge>
          )}

          <span className="ml-auto shrink-0 text-xs text-ink-500">
            {formatOrderDate(order.createdAt)}
          </span>
        </div>

        {/* Menu Info */}
        <div className="flex items-baseline justify-between gap-3">
          <span className="min-w-0 truncate text-sm text-ink-700">
            {order.menuName}
          </span>
          <span className="shrink-0 text-base font-bold text-ink-900">
            {order.price.toLocaleString("ko-KR")}원
          </span>
        </div>

        {/* Review Button */}
        <ReviewButton
          reviewDeadline={order.reviewDeadline}
          hasReviewBadge={order.hasReviewBadge}
          reviewStatus={order.reviewStatus}
          onReviewClick={() => setIsReviewModalOpen(true)}
        />
      </Card>

      <ReviewModal
        open={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        orderId={order.id}
        storeName={order.storeName}
        menuName={order.menuName}
        onSubmitSuccess={() => {
          // 서버에 저장이 끝난 뒤에만 불린다. 새로고침해도 같은 상태로 온다.
          setOrder({ ...order, reviewStatus: 'done' });
        }}
      />
    </>
  );
}
