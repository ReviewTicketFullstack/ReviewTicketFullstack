/**
 * 주문 내역의 단일 항목 행. 메뉴명 / 수량 / 가격 1행 구조.
 * Phase 5 스캐폴드 — 데이터 연결 없이 자리표시자만 렌더한다.
 */
export function OrderItemRow() {
  return (
    <div className="flex items-baseline gap-3">
      <span className="min-w-0 flex-1 truncate text-sm text-ink-900">메뉴 이름</span>
      <span className="shrink-0 text-sm text-ink-500">수량</span>
      <span className="shrink-0 text-sm text-ink-900">가격</span>
    </div>
  );
}
