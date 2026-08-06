interface OrderItemBoxProps {
  menuName: string;
  price: number;
}

// 주문 메뉴/가격을 빨간 테두리 박스로 표시 (리뷰완료/리뷰미작성 카드 공통)
export function OrderItemBox({ menuName, price }: OrderItemBoxProps) {
  return (
    <div className="inline-flex w-fit items-center gap-2 rounded-full border border-brand-800 px-3 py-1 text-xs">
      <span className="text-brand-800 ">{menuName}</span>
      <span className="font-bold text-brand-800">{price.toLocaleString('ko-KR')}원</span>
    </div>
  );
}
