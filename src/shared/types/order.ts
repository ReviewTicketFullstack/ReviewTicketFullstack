/**
현재 사용 중인 Order 임시 모델.
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
