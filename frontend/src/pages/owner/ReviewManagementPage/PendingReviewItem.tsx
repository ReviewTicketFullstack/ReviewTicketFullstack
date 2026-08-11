import { useEffect, useState } from 'react';
import { Card } from '@/shared/ui';
import type { PendingOrder } from '@/api/reviewApi';
import { OrderItemBox } from './OrderItemBox';
import {
  getRemainingReviewTime,
  formatTimeRemaining,
} from '@/entities/order/reviewTime';

interface PendingReviewItemProps {
  order: PendingOrder;
}

export function PendingReviewItem({ order }: PendingReviewItemProps) {
  // 리뷰 미작성 주문 1건 카드
  // 주문일자를 한국어 표기로 변환 (CompletedReviewItem과 동일한 포맷)
  const date = new Date(order.orderedAt).toLocaleDateString('ko-KR');

  // 작성 대기 카드만 카운트다운을 돈다. 미이행은 이미 마감이라 셀 것이 없다.
  const isPending = order.reviewStatus === 'pending';
  const [remaining, setRemaining] = useState(() =>
    getRemainingReviewTime(order.expireTime),
  );

  useEffect(() => {
    if (!isPending) return;
    // 손님쪽 ReviewButton 과 같은 주기다. 1초로 두면 초 표시가 가끔 건너뛴다.
    const interval = setInterval(() => {
      setRemaining(getRemainingReviewTime(order.expireTime));
    }, 300);
    return () => clearInterval(interval);
  }, [isPending, order.expireTime]);

  return (
    <Card bordered className="flex flex-col gap-3 p-4">
      <div className="flex items-center gap-3 text-sm">
        <span className="font-semibold text-ink-900">{order.displayName}</span>
        <span className="text-ink-500">{date}</span>
        {isPending && (
          <span className="ml-auto font-semibold text-orange-600">
            {remaining > 0 ? `${formatTimeRemaining(remaining)} 남음` : '마감'}
          </span>
        )}
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
