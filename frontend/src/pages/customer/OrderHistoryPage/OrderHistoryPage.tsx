import { useEffect, useState } from 'react';
import { getMyOrders } from '@/api/orderApi';
import { getOrderHistory, replaceOrderHistory } from '@/entities/order/orderStorage';
import { ApiError } from '@/shared/api';
import { EmptyState, Loading } from '@/shared/ui';
import { OrderHistoryItem } from './OrderHistoryItem';
import type { Order } from '@/entities/order';

export function OrderHistoryPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    getMyOrders(controller.signal)
      .then((serverOrders) => {
        setOrders(serverOrders);
        // 다음에 서버가 응답하지 못할 때 보여줄 사본을 갱신해 둔다.
        replaceOrderHistory(serverOrders);
      })
      .catch((e: unknown) => {
        // 요청 취소(StrictMode 의 이중 마운트)는 실패가 아니다.
        if (e instanceof DOMException && e.name === 'AbortError') return;

        // 서버에 닿지 못하면 사본이라도 보여준다. 빈 화면보다는 낫다.
        const cached = getOrderHistory();
        setOrders(cached);
        setError(
          cached.length > 0
            ? '서버에 연결하지 못해 저장된 내역을 보여줍니다.'
            : e instanceof ApiError
              ? e.message
              : '주문내역을 불러오지 못했습니다.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, []);

  return (
    <div className="flex flex-col gap-8 px-5 py-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-xl font-bold text-ink-900">주문내역</h1>
        <p className="text-sm text-ink-700">완료된 주문을 확인할 수 있어요.</p>
      </div>

      {isLoading ? (
        <Loading />
      ) : (
        <>
          {error && (
            <p className="rounded-lg bg-fill-100 px-3 py-3 text-sm text-ink-700">
              {error}
            </p>
          )}

          {orders.length === 0 ? (
            <EmptyState
              icon="🧾"
              message="아직 주문한 내역이 없어요. 마음에 드는 가게에서 첫 주문을 해보세요."
            />
          ) : (
            <ul className="flex flex-col gap-3">
              {orders.map((order) => (
                <li key={order.id}>
                  <OrderHistoryItem order={order} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
