import type { Order } from '@/shared/types/order';

const ORDERS_KEY = 'review_ticket_orders';
const REVIEW_TIME_LIMIT_MS = 60 * 1000; // 1 minute

export function saveOrder(order: Omit<Order, 'id' | 'createdAt' | 'reviewDeadline' | 'reviewStatus'>): Order {
  const orders = getOrderHistory();

  const now = new Date();
  const createdAt = now.toISOString().split('T')[0];
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

  const orders = JSON.parse(stored) as Order[];
  return orders.map((order) => ({
    ...order,
    reviewStatus: order.reviewStatus || 'available',
  }));
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
