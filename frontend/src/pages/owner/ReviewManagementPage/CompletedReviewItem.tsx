import { Card, StarRating } from '@/shared/ui';
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
        <span className="font-bold text-ink-900">{review.displayName}</span>
        <StarRating rating={review.reviewRating} />
        <span className="ml-auto shrink-0 text-xs text-ink-500">{date}</span>
      </div>
      <p className="text-sm leading-relaxed text-ink-700">{review.reviewContent}</p>
      {/* 리뷰 사진은 AI 검증을 통과해야 저장되므로 실질적으로 항상 존재하지만, 백엔드 계약 변경에 대비해 체크 유지 */}
      {review.reviewImageUrl && (
        <img
          src={review.reviewImageUrl}
          alt=""
          className="size-24 rounded-xl bg-fill-100 object-cover"
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
