/** 서버 발급 식별자 */
export type ID = number;
/** ISO 8601 UTC 문자열. 예: "2026-08-05T04:20:00Z" */
export type ISODateTime = string;

/**
 * 리뷰 작성 상태.
 *
 * 서버는 "available" / "not_available" 두 가지만 보낸다.
 * "pending"(작성 후 검토중)은 리뷰 등록 API 가 생기기 전까지 화면에서만 쓴다.
 */
export type ReviewStatus = "available" | "pending" | "not_available";

/** 주문내역 카드 한 장. 백엔드 OrderResponse 와 1:1. */
export interface Order {
  id: ID;
  storeId: ID;
  /** 주문 시점 스냅샷. 가게 이름이 나중에 바뀌어도 그대로다 */
  storeName: string;
  /** 주문 시점 스냅샷 */
  menuName: string;
  /** 원 단위 정수 */
  price: number;
  /** 리뷰 버튼을 띄울지 (주문 시점의 리뷰이벤트 여부) */
  hasReviewBadge: boolean;
  reviewStatus: ReviewStatus;
  /** 리뷰 마감 시각. 남은 시간 카운트다운에 쓴다 */
  reviewDeadline: ISODateTime;
  createdAt: ISODateTime;
}
