import { Badge, Card } from '@/shared/ui';

/**
 * 리뷰 단일 아이템. 별점 / 작성일 / 심사 상태 / 본문 구조.
 * Phase 5 스캐폴드 — 데이터 연결 없이 자리표시자만 렌더한다.
 * 클릭 대상이 아니므로 그림자 없이 Border로 구분한다.
 */
export function ReviewCard() {
  return (
    <Card bordered className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <span className="flex items-center gap-1">
          <svg
            viewBox="0 0 20 20"
            fill="currentColor"
            aria-hidden="true"
            className="size-3.5 shrink-0 text-star"
          >
            <path d="M10 2.6l2.3 4.66 5.15.75-3.73 3.63.88 5.12L10 14.34l-4.6 2.42.88-5.12L2.55 8.01l5.15-.75L10 2.6Z" />
          </svg>
          <span className="text-sm font-bold text-ink-900">별점</span>
        </span>
        <span className="text-xs text-ink-500">작성일</span>
        <Badge variant="info" className="ml-auto shrink-0">
          심사 상태
        </Badge>
      </div>

      <p className="text-sm text-ink-700">리뷰 내용</p>
    </Card>
  );
}
