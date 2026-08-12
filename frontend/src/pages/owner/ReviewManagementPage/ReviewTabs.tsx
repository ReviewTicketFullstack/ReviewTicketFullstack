import { RotateCw } from 'lucide-react';

export type ReviewTab = 'completed' | 'pending' | 'expired';

interface ReviewTabsProps {
  active: ReviewTab;
  onChange: (tab: ReviewTab) => void;
  onRefresh: () => void;
  isRefreshing?: boolean;
}

export function ReviewTabs({
  active,
  onChange,
  onRefresh,
  isRefreshing = false,
}: ReviewTabsProps) {
  return (
    // {/* 리뷰 탭 네비게이션 */} 
    <div className="flex gap-4 border-b border-neutral-200">
      {/* 리뷰완료 탭 */} 
      <button
        type="button"
        onClick={() => onChange('completed')}
        className={
          active === 'completed'
            ? 'border-b-2 border-brand-800 pb-2 font-bold text-brand-800'
            : 'pb-2 text-neutral-500'
        }
      >
        리뷰완료
      </button>
      {/* 미이행 탭 — 리뷰 기한이 지나버린 주문 */}
      <button
        type="button"
        onClick={() => onChange('expired')}
        className={
          active === 'expired'
            ? 'border-b-2 border-brand-800 pb-2 font-bold text-brand-800'
            : 'pb-2 text-neutral-500'
        }
      >
        미이행
      </button>
      {/* 작성 대기 탭 — 마감 전이라 아직 리뷰가 들어올 수 있다 */}
      <button
        type="button"
        onClick={() => onChange('pending')}
        className={
          active === 'pending'
            ? 'border-b-2 border-brand-800 pb-2 font-bold text-brand-800'
            : 'pb-2 text-neutral-500'
        }
      >
        작성 대기
      </button>
      {/* 목록·개수를 다시 받아온다. 마감이 지나 미이행으로 넘어간 건은
          재조회해야 탭이 옮겨간다 */}
      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing}
        aria-label="새로고침"
        className="ml-auto flex size-8 items-center justify-center rounded-lg text-neutral-500 hover:bg-neutral-100 disabled:opacity-50"
      >
        <RotateCw size={16} className={isRefreshing ? 'animate-spin' : ''} />
      </button>
    </div>
  );
}
