import { useEffect, useState } from 'react';
import { getOrderHistory } from '@/features/order/orderStorage';
import { OrderHistoryItem } from './OrderHistoryItem';
import type { Order } from '@/types/order';

export function OrderHistoryPage() {
  const [orders, setOrders] = useState<Order[]>([]);

  useEffect(() => {
    const savedOrders = getOrderHistory();
    setOrders(savedOrders);
  }, []);

  return (
    <div className="space-y-6 px-5 py-6">
      <div>
        <h1 className="text-2xl font-bold">주문내역</h1>
        <p className="text-gray-600">완료된 주문을 확인할 수 있습니다.</p>
      </div>

      {orders.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500">주문내역이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <OrderHistoryItem key={order.id} order={order} />
          ))}
        </div>
      )}
    </div>
  );
}
