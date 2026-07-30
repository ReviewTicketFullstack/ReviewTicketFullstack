import { Children, type ReactNode } from 'react';
import { Card } from '@/shared/ui';

export interface OrderSummaryProps {
  /** OrderItem들을 children으로 조합한다. */
  children?: ReactNode;
}

/**
 * 주문 요약 카드. 항목 목록과 합계 줄의 골격만 잡는다.
 * 클릭 대상이 아니므로 그림자 없이 Border로 구분한다.
 */
export function OrderSummary({ children }: OrderSummaryProps) {
  return (
    <Card bordered className="flex flex-col gap-3">
      <h3 className="text-base font-bold text-ink-900">주문 내역</h3>

      {Children.toArray(children).length > 0 && (
        <ul className="flex flex-col gap-2">
          {Children.map(children, (child) => (
            <li>{child}</li>
          ))}
        </ul>
      )}

      <div className="flex items-baseline justify-between border-t border-line-100 pt-3">
        <span className="text-sm text-ink-700">총 결제 금액</span>
        <span className="text-base font-bold text-ink-900">금액</span>
      </div>
    </Card>
  );
}
