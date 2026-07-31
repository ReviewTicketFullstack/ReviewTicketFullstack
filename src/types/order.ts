/**
 * [TEMPORARY] 임시 Order 모델.
 *
 * UI 프로토타입 단계에서 사용 중입니다.
 * 평면화 구조: 주문 1건 = 메뉴 1개 지원
 *
 * 향후 정규화될 예정 (items: OrderLine[] 구조)
 */

export type ReviewStatus = "available" | "pending" | "not_available";

export interface Order {
  id: string;
  storeId: string;
  storeName: string;
  menuName: string;
  price: number;
  hasReviewBadge: boolean;
  reviewStatus: ReviewStatus;
  createdAt: string;
  reviewDeadline: string;
}

/**
 * [DEPRECATED] 정규화 구조. 현재 사용되지 않습니다.
 * 향후 참고 기준으로 남깁니다.
 */
/*
export interface OrderItem {
  menuItemId: string;
  menuName: string;
  price?: number;
}
*/
