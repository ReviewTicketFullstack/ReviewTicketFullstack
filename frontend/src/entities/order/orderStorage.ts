import type { Order } from './model';

/**
 * 최근 주문의 로컬 사본.
 *
 * 서버가 진짜 소스다. 이 사본은 서버에 닿지 못할 때만 쓴다 — 서버 점검이나
 * 네트워크 문제로 주문내역이 통째로 비어 보이는 것을 막는다.
 *
 * 프론트가 값을 만들지 않고 서버 응답을 그대로 저장한다. id·가격·마감 시각을
 * 여기서 따로 만들면 서버 값과 어긋나고, 어느 쪽이 맞는지 판단할 방법이 없다.
 */

const ORDERS_KEY = 'review_ticket_orders';

/** 방금 만든 주문 하나를 사본 맨 앞에 넣는다. 서버 응답을 그대로 받는다. */
export function saveOrder(order: Order): void {
  const orders = getOrderHistory().filter((saved) => saved.id !== order.id);
  orders.unshift(order);
  writeOrders(orders);
}

/** 서버에서 받은 목록으로 사본을 통째로 갱신한다. */
export function replaceOrderHistory(orders: Order[]): void {
  writeOrders(orders);
}

export function getOrderHistory(): Order[] {
  const stored = localStorage.getItem(ORDERS_KEY);
  if (!stored) return [];

  try {
    const parsed: unknown = JSON.parse(stored);
    if (!Array.isArray(parsed)) throw new Error('주문 사본이 배열이 아닙니다.');

    return parsed as Order[];
  } catch {
    // 손상된 값이 남아 이후 조회까지 계속 실패하지 않도록 비운다.
    localStorage.removeItem(ORDERS_KEY);
    return [];
  }
}

function writeOrders(orders: Order[]): void {
  try {
    localStorage.setItem(ORDERS_KEY, JSON.stringify(orders));
  } catch {
    // 저장이 막힌 환경(사생활 보호 모드 등)에서는 사본 없이 동작한다.
  }
}
