/**
 * 주문 가능 가게. 기능명세 FE_4 의 가게 목록 항목이다.
 *
 * [TODO] 필드명은 백엔드 API 명세 확정 전 임시다. GET /api/stores 명세가 나오면 맞춘다.
 */
export interface Store {
  id: string;
  name: string;
  imageUrl?: string;
  /** 평균 별점 */
  rating: number;
  reviewCount: number;
  /** 리뷰 작성 가능 메뉴가 하나라도 있으면 true — 리뷰이벤트 뱃지 노출 조건 */
  hasReviewEvent: boolean;
}
