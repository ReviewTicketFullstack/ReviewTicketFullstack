import { useEffect, useState } from 'react';
import { getMyOrders } from '@/api/orderApi';
import { ApiError } from '@/shared/api';
import { OrderHistoryItem } from './OrderHistoryItem';
import type { Order } from '@/entities/order';

export function OrderHistoryPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    getMyOrders(controller.signal)
      .then(setOrders)
      .catch((e: unknown) => {
        // 요청 취소(StrictMode 의 이중 마운트)는 실패가 아니다.
        if (e instanceof DOMException && e.name === 'AbortError') return;
        setError(
          e instanceof ApiError ? e.message : '주문내역을 불러오지 못했습니다.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, []);

  return (
    <div className="space-y-6 px-5 py-6">
      <div>
        <h1 className="text-2xl font-bold">주문내역</h1>
        <p className="text-gray-600">완료된 주문을 확인할 수 있습니다.</p>
      </div>

      {isLoading ? (
        <div className="text-center py-12">
          <p className="text-gray-500">불러오는 중...</p>
        </div>
      ) : error ? (
        <div className="text-center py-12">
          <p className="text-red-600">{error}</p>
        </div>
      ) : orders.length === 0 ? (
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
