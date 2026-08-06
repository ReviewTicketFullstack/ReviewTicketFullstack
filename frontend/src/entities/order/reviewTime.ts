import type { ISODateTime } from './model';

/** 리뷰 마감까지 남은 시간(ms). 지났으면 0. */
export function getRemainingReviewTime(reviewDeadline: ISODateTime): number {
  const now = new Date().getTime();
  const deadline = new Date(reviewDeadline).getTime();
  return Math.max(0, deadline - now);
}

/** 남은 시간을 "MM:SS" 로. 카운트다운 표시용. */
export function formatTimeRemaining(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * 서버가 주는 ISO UTC 시각을 주문내역에 보여줄 날짜로.
 *
 * 서버는 기준이 분명한 UTC 로 보내고, 사람이 읽는 형식으로 바꾸는 것은
 * 표시 직전에만 한다. toLocaleDateString 이 브라우저 시간대(KST)를 적용한다.
 */
export function formatOrderDate(createdAt: ISODateTime): string {
  return new Date(createdAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}
