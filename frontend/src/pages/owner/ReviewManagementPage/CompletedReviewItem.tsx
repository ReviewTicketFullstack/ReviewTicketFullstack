import { Card } from '@/shared/ui';
import type { OwnerReview } from '@/api/reviewApi';
import { OrderItemBox } from './OrderItemBox';

interface CompletedReviewItemProps {
  review: OwnerReview;
}

export function CompletedReviewItem({ review }: CompletedReviewItemProps) {
  // 서버 날짜 문자열을 한국어 표기(YYYY. M. D.)로 변환
  const date = new Date(review.reviewCreatedAt).toLocaleDateString('ko-KR');

  return (
    <Card bordered className="flex flex-col gap-2 p-4">
      <div className="flex items-center gap-3 text-sm">
        <span className="font-semibold text-ink-900">{review.displayName}</span>
        {/* 별점 숫자만큼 별 아이콘 반복 표시 */}
        <span className="flex items-center gap-0.5">
          {Array.from({ length: review.reviewRating }).map((_, i) => (
            <img key={i} src="/star.svg" alt="" className="size-3.5" />
          ))}
        </span>
        <span className="text-ink-500">{date}</span>
      </div>
      <p className="text-sm text-ink-700">{review.reviewContent}</p>
      {/* 리뷰 사진은 AI 검증을 통과해야 저장되므로 항상 한 장 있다 */}
      {review.reviewImageUrl && (
        <img
          src={review.reviewImageUrl}
          alt=""
          className="h-24 w-24 rounded-md object-cover"
        />
      )}
      {/* 리뷰 작성한 주문의 메뉴/가격. 리뷰가 달렸다는 것은 이벤트 참여 주문이라는 뜻이다 */}
      <OrderItemBox
        menuName={review.menuName}
        price={review.menuPrice}
        hasReviewEvent
      />
    </Card>
  );
}
