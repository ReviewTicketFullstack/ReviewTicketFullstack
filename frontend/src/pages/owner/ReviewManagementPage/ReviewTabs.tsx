export type ReviewTab = 'completed' | 'pending' | 'expired';

interface ReviewTabsProps {
  active: ReviewTab;
  onChange: (tab: ReviewTab) => void;
}

export function ReviewTabs({ active, onChange }: ReviewTabsProps) {
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
    </div>
  );
}
