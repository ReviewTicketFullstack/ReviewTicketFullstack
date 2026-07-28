import { Children, type ReactNode } from 'react';
import { EmptyState } from '@/shared/ui';

export interface MenuListProps {
  /** MenuCard들을 children으로 조합한다. */
  children?: ReactNode;
}

/**
 * 메뉴 목록 컨테이너. ul/li 마크업과 아이템 간격만 책임진다.
 */
export function MenuList({ children }: MenuListProps) {
  if (Children.toArray(children).length === 0) {
    return <EmptyState message="등록된 메뉴가 아직 없어요" />;
  }

  return (
    <ul className="flex flex-col gap-3">
      {Children.map(children, (child) => (
        <li>{child}</li>
      ))}
    </ul>
  );
}
