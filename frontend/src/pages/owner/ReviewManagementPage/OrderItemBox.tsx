interface OrderItemBoxProps {
  menuName: string;
  price: number;
  hasReviewEvent: boolean;
}

// 주문 메뉴/가격을 빨간 테두리 박스로 표시 (리뷰완료/리뷰미작성 카드 공통)
export function OrderItemBox({ menuName, price, hasReviewEvent }: OrderItemBoxProps) {
  return (
    <div className="inline-flex w-fit items-center gap-1 rounded-full border border-brand-800 px-3 py-1 text-xs">
      <span className="text-brand-800 ">{menuName}</span>
      {/* 메뉴관리에서 리뷰이벤트 설정된 메뉴만 뱃지 표시 (MenuListItem과 동일 스타일) */}
      {hasReviewEvent && (
        <span className="rounded bg-red-700 px-2 py-0.5 text-xs font-semibold text-white">
          리뷰
        </span>
      )}
      <span className="ml-1 font-bold text-brand-800">{price.toLocaleString('ko-KR')}원</span>
    </div>
  );
}
