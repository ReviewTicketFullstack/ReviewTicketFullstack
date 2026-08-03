import { Card } from '@/shared/ui';
import type { CompletedReview } from './mockData';

interface CompletedReviewItemProps {
  review: CompletedReview;
}

export function CompletedReviewItem({ review }: CompletedReviewItemProps) {
  const date = new Date(review.createdAt).toLocaleDateString('ko-KR');

  return (
    <Card bordered className="flex flex-col gap-2 p-4">
      <div className="flex items-center gap-3 text-sm">
        <span className="font-semibold text-ink-900">{review.reviewerId}</span>
        <span className="text-star">{'★'.repeat(review.rating)}</span>
        <span className="text-ink-500">{date}</span>
      </div>
      <p className="text-sm text-ink-700">{review.comment}</p>
      {review.photo && (
        <img
          src={review.photo}
          alt="리뷰 사진"
          className="h-24 w-24 rounded-md object-cover"
        />
      )}
    </Card>
  );
}
