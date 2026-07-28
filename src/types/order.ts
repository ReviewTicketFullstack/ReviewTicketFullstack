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
