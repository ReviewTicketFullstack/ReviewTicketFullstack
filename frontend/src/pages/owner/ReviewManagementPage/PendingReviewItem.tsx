import { Card } from '@/shared/ui';
import type { PendingOrder } from './mockData';
import { OrderItemBox } from './OrderItemBox';
import { menuItems } from '../MenuManagementPage/mockData';

interface PendingReviewItemProps {
  order: PendingOrder;
}

export function PendingReviewItem({ order }: PendingReviewItemProps) {
  // 리뷰 미작성 주문 1건 카드
  // 주문일자를 한국어 표기로 변환 (CompletedReviewItem과 동일한 포맷)
  const date = new Date(order.createdAt).toLocaleDateString('ko-KR');
  // 메뉴관리에서 설정한 리뷰이벤트 여부를 menuId로 찾아 실시간 반영
  const menu = menuItems.find((item) => item.id === order.menuId);

  return (
    <Card bordered className="flex flex-col gap-3 p-4">
      <div className="flex items-center gap-3 text-sm">
        <span className="font-semibold text-ink-900">{order.ordererId}</span>
        <span className="text-ink-500">{date}</span>
      </div>
      {/* 주문한 메뉴/가격 */}
      <OrderItemBox
        menuName={order.menuName}
        price={order.price}
        hasReviewEvent={menu?.hasReviewEvent ?? false}
      />
    </Card>
  );
}
