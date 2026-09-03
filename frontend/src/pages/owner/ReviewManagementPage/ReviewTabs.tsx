import { RotateCw } from 'lucide-react';

export type ReviewTab = 'completed' | 'pending' | 'expired';

interface ReviewTabsProps {
  active: ReviewTab;
  onChange: (tab: ReviewTab) => void;
  onRefresh: () => void;
  isRefreshing?: boolean;
}

const TABS: { id: ReviewTab; label: string }[] = [
  { id: 'completed', label: '리뷰완료' },
  { id: 'expired', label: '미이행' },
  { id: 'pending', label: '작성 대기' },
];

export function ReviewTabs({
  active,
  onChange,
  onRefresh,
  isRefreshing = false,
}: ReviewTabsProps) {
  return (
    <div role="tablist" className="flex items-center gap-1 border-b border-line-100">
      {TABS.map(({ id, label }) => {
        const isActive = active === id;

        return (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(id)}
            className={`-mb-px h-11 border-b-2 px-3 text-sm font-semibold transition-colors ${
              isActive
                ? 'border-brand-800 text-brand-800'
                : 'border-transparent text-ink-500 hover:text-ink-700'
            }`}
          >
            {label}
          </button>
        );
      })}

      {/* 목록·개수를 다시 받아온다. 마감이 지나 미이행으로 넘어간 건은
          재조회해야 탭이 옮겨간다 */}
      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing}
        aria-label="새로고침"
        className="ml-auto flex size-11 items-center justify-center rounded-lg text-ink-500 transition-colors hover:bg-fill-100 active:bg-line-100 disabled:text-ink-300"
      >
        <RotateCw
          size={16}
          aria-hidden="true"
          className={isRefreshing ? 'animate-spin motion-reduce:animate-none' : ''}
        />
      </button>
    </div>
  );
}
