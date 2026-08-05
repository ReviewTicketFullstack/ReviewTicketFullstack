import { Card } from '@/shared/ui';
import type { PendingOrder } from './mockData';

interface PendingReviewItemProps {
  order: PendingOrder;
}

export function PendingReviewItem({ order }: PendingReviewItemProps) {
  // 리뷰 미작성 주문 1건 카드
  return (
    <Card bordered className="flex flex-col gap-1 p-4">
      <span className="font-semibold text-ink-900">{order.ordererId}</span>
      <span className="text-sm text-ink-700">
        {order.menuName} · {order.price.toLocaleString('ko-KR')}원
      </span>
    </Card>
  );
}
