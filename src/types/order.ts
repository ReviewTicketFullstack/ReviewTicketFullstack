/**
 * 정규화된 Order 모델.
 * 현재 사용 중인 모델은 @/shared/types/order.ts 를 참조하세요.
 */

// export type OrderStatus = 'pending' | 'completed' | 'cancelled';

export interface OrderItem {
  menuItemId: string;
  menuName: string;
  /** Snapshot of MenuItem.price at the time the order was placed. */
  price?: number;
}

export interface Order {
  id: string;
  storeId: string;
  items: OrderItem[];
  status: string; // 추후 OrderStatus
  createdAt: string;
}
