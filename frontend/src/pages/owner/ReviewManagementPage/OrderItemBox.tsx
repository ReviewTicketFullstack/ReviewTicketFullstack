import { Badge } from '@/shared/ui';

interface OrderItemBoxProps {
  menuName: string;
  price: number;
  hasReviewEvent: boolean;
}

// 주문 메뉴/가격을 빨간 테두리 박스로 표시 (리뷰완료/리뷰미작성 카드 공통)
export function OrderItemBox({ menuName, price, hasReviewEvent }: OrderItemBoxProps) {
  return (
    <div className="inline-flex w-fit items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-xs">
      <span className="font-semibold text-brand-900">{menuName}</span>
      {/* 메뉴관리에서 리뷰이벤트 설정된 메뉴만 뱃지 표시 (MenuListItem과 동일 스타일) */}
      {hasReviewEvent && <Badge variant="accent">리뷰</Badge>}
      <span className="font-bold text-brand-900">
        {price.toLocaleString('ko-KR')}원
      </span>
    </div>
  );
}
