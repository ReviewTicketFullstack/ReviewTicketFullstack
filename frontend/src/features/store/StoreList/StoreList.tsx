import { Children, type ReactNode } from 'react';
import { EmptyState } from '@/shared/ui';

export interface StoreListProps {
  /** StoreCard들을 children으로 조합한다. */
  children?: ReactNode;
}

/**
 * 가게 목록 컨테이너. ul/li 마크업과 아이템 간격만 책임진다.
 * 구분선 없이 gap-3으로 나눈다.
 */
export function StoreList({ children }: StoreListProps) {
  if (Children.toArray(children).length === 0) {
    return <EmptyState message="주변에 등록된 가게가 아직 없어요" />;
  }

  return (
    <ul className="flex flex-col gap-3">
      {Children.map(children, (child) => (
        <li>{child}</li>
      ))}
    </ul>
  );
}
