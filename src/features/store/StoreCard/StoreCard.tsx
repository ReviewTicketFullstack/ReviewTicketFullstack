import { Badge, Card } from '@/shared/ui';

/**
 * 가게 목록의 단일 아이템. 좌측 정사각 미디어 + 우측 텍스트 3줄 구조.
 * Phase 5 스캐폴드 — 데이터 연결 없이 자리표시자만 렌더한다.
 * Card interactive는 button으로 렌더되므로 내부는 phrasing content(span/svg)만 둔다.
 */
export function StoreCard() {
  return (
    <Card interactive className="flex w-full items-center gap-3">
      <span
        aria-hidden="true"
        className="flex size-20 shrink-0 items-center justify-center rounded-xl bg-fill-100 text-ink-300"
      >
        <svg viewBox="0 0 20 20" fill="currentColor" className="size-7">
          <path d="M2.6 8.8h14.8a1 1 0 0 1 1 1.05A8.35 8.35 0 0 1 15 15.4H5a8.35 8.35 0 0 1-3.4-5.55 1 1 0 0 1 1-1.05Z" />
          <rect x="3" y="16" width="14" height="2" rx="1" />
        </svg>
      </span>

      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="flex min-w-0 items-center gap-2">
          <span className="truncate text-base font-bold text-ink-900">가게 이름</span>
          <Badge variant="accent" className="shrink-0">
            리뷰이벤트
          </Badge>
        </span>
        <span className="text-xs text-ink-500">별점 · 리뷰 수</span>
        <span className="text-xs text-ink-500">가게 소개</span>
      </span>
    </Card>
  );
}
