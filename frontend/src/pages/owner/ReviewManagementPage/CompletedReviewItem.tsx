import { Card } from '@/shared/ui';
import type { CompletedReview } from './mockData';

interface CompletedReviewItemProps {
  review: CompletedReview;
}

export function CompletedReviewItem({ review }: CompletedReviewItemProps) {
  // 서버 날짜 문자열을 한국어 표기(YYYY. M. D.)로 변환
  const date = new Date(review.createdAt).toLocaleDateString('ko-KR');

  return (
    <Card bordered className="flex flex-col gap-2 p-4">
      <div className="flex items-center gap-3 text-sm">
        <span className="font-semibold text-ink-900">{review.reviewerId}</span>
        {/* 별점 숫자를 ★ 문자 반복으로 표시 */}
        <span className="text-star">{'★'.repeat(review.rating)}</span>
        <span className="text-ink-500">{date}</span>
      </div>
      <p className="text-sm text-ink-700">{review.comment}</p>
      {/* 사진 있는 리뷰만 이미지 자리 표시 */}
      {review.photo && (
        <div className="flex h-24 w-24 items-center justify-center rounded-md bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>
      )}
    </Card>
  );
}
