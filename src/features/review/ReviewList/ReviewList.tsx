import { Children, type ReactNode } from 'react';
import { EmptyState } from '@/shared/ui';

export interface ReviewListProps {
  /** ReviewCard들을 children으로 조합한다. */
  children?: ReactNode;
}

/**
 * 리뷰 목록 컨테이너. ul/li 마크업과 아이템 간격만 책임진다.
 */
export function ReviewList({ children }: ReviewListProps) {
  if (Children.toArray(children).length === 0) {
    return <EmptyState message="아직 작성된 리뷰가 없어요" />;
  }

  return (
    <ul className="flex flex-col gap-3">
      {Children.map(children, (child) => (
        <li>{child}</li>
      ))}
    </ul>
  );
}
