export type ReviewTab = 'completed' | 'pending';

interface ReviewTabsProps {
  active: ReviewTab;
  onChange: (tab: ReviewTab) => void;
}

export function ReviewTabs({ active, onChange }: ReviewTabsProps) {
  return (
    <div className="flex gap-4 border-b border-neutral-200">
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
      <button
        type="button"
        onClick={() => onChange('pending')}
        className={
          active === 'pending'
            ? 'border-b-2 border-brand-800 pb-2 font-bold text-brand-800'
            : 'pb-2 text-neutral-500'
        }
      >
        리뷰미작성
      </button>
    </div>
  );
}
