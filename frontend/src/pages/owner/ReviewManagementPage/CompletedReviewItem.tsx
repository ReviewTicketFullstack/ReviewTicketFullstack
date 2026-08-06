import { Card } from '@/shared/ui';
import type { CompletedReview } from './mockData';
import { OrderItemBox } from './OrderItemBox';

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
        {/* 별점 숫자만큼 별 아이콘 반복 표시 */}
        <span className="flex items-center gap-0.5">
          {Array.from({ length: review.rating }).map((_, i) => (
            <img key={i} src="/star.svg" alt="" className="size-3.5" />
          ))}
        </span>
        <span className="text-ink-500">{date}</span>
      </div>
      <p className="text-sm text-ink-700">{review.comment}</p>
      {/* 사진 있는 리뷰만 이미지 자리 표시 */}
      {review.photo && (
        <div className="flex h-24 w-24 items-center justify-center rounded-md bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>
      )}
      {/* 리뷰 작성한 주문의 메뉴/가격 */}
      <OrderItemBox
        menuName={review.menuName}
        price={review.price}
        hasReviewEvent={review.hasReviewEvent}
      />
    </Card>
  );
}
