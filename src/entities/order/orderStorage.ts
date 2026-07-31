import type { Order } from './model';

const ORDERS_KEY = 'review_ticket_orders';
const REVIEW_TIME_LIMIT_MS = 60 * 1000; // 1 minute

// toISOString은 UTC 기준이라 KST 새벽 주문이 전날로 기록된다. 로컬 날짜로 포맷한다.
function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function saveOrder(order: Omit<Order, 'id' | 'createdAt' | 'reviewDeadline' | 'reviewStatus'>): Order {
  const orders = getOrderHistory();

  const now = new Date();
  const createdAt = formatLocalDate(now);
  const reviewDeadlineTime = new Date(now.getTime() + REVIEW_TIME_LIMIT_MS);
  const reviewDeadline = reviewDeadlineTime.toISOString();

  const newOrder: Order = {
    ...order,
    id: `order-${Date.now()}`,
    createdAt,
    reviewDeadline,
    reviewStatus: 'available',
  };

  orders.unshift(newOrder);
  localStorage.setItem(ORDERS_KEY, JSON.stringify(orders));

  return newOrder;
}

export function getOrderHistory(): Order[] {
  const stored = localStorage.getItem(ORDERS_KEY);
  if (!stored) return [];

  try {
    const parsed: unknown = JSON.parse(stored);
    if (!Array.isArray(parsed)) throw new Error('주문 내역이 배열이 아닙니다.');

    return (parsed as Order[]).map((order) => ({
      ...order,
      reviewStatus: order.reviewStatus || 'available',
    }));
  } catch {
    // 저장된 값이 손상된 경우 비워서 이후 조회까지 계속 실패하지 않도록 한다.
    localStorage.removeItem(ORDERS_KEY);
    return [];
  }
}

export function clearOrderHistory(): void {
  localStorage.removeItem(ORDERS_KEY);
}

export function updateOrderReviewStatus(orderId: string, status: 'available' | 'pending' | 'not_available'): void {
  const orders = getOrderHistory();
  const updated = orders.map((order) =>
    order.id === orderId ? { ...order, reviewStatus: status } : order
  );
  localStorage.setItem(ORDERS_KEY, JSON.stringify(updated));
}

export function getRemainingReviewTime(reviewDeadline: string): number {
  const now = new Date().getTime();
  const deadline = new Date(reviewDeadline).getTime();
  return Math.max(0, deadline - now);
}

export function formatTimeRemaining(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}
