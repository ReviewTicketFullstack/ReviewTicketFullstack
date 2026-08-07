import { Card } from '@/shared/ui';
import type { PendingOrder } from '@/api/reviewApi';
import { OrderItemBox } from './OrderItemBox';

interface PendingReviewItemProps {
  order: PendingOrder;
}

export function PendingReviewItem({ order }: PendingReviewItemProps) {
  // 리뷰 미작성 주문 1건 카드
  // 주문일자를 한국어 표기로 변환 (CompletedReviewItem과 동일한 포맷)
  const date = new Date(order.orderedAt).toLocaleDateString('ko-KR');

  return (
    <Card bordered className="flex flex-col gap-3 p-4">
      <div className="flex items-center gap-3 text-sm">
        <span className="font-semibold text-ink-900">{order.displayName}</span>
        <span className="text-ink-500">{date}</span>
      </div>
      {/* 주문한 메뉴/가격. 이 목록은 이벤트 참여 주문만 담고 있다 */}
      <OrderItemBox
        menuName={order.menuName}
        price={order.menuPrice}
        hasReviewEvent
      />
    </Card>
  );
}
