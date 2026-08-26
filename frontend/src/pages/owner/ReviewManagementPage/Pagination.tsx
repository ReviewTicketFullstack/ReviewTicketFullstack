import { Button } from '@/shared/ui';

interface PaginationProps {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

/** 목록 하단 페이지 이동. 총 페이지가 1 이하면 아무것도 렌더링하지 않는다. */
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-2">
      <Button variant="ghost" size="small" onClick={() => onChange(page - 1)} disabled={page <= 1}>
        이전
      </Button>
      <span className="text-xs font-semibold text-neutral-600">
        {page} / {totalPages}
      </span>
      <Button
        variant="ghost"
        size="small"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
      >
        다음
      </Button>
    </div>
  );
}
