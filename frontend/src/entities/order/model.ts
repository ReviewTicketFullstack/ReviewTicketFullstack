/** 서버 발급 식별자 */
export type ID = number;
/** ISO 8601 UTC 문자열. 예: "2026-08-05T04:20:00Z" */
export type ISODateTime = string;

/**
 * 리뷰 작성 상태. 서버가 조회 시점에 계산해서 내려준다.
 *
 * - not_available: 리뷰이벤트에 참여하지 않은 주문
 * - available: 지금 리뷰를 쓸 수 있다
 * - expired: 마감이 지났고 리뷰도 없다
 * - done: 리뷰를 이미 썼다
 *
 * "검토중" 상태는 없다 — AI 검증이 제출과 동시에 끝나 승인을 기다리는 구간이 생기지 않는다.
 */
export type ReviewStatus = "available" | "not_available" | "expired" | "done";

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
  /** 리뷰 마감 시각. 남은 시간 카운트다운에 쓴다. 이벤트 미참여 주문은 null */
  reviewDeadline: ISODateTime | null;
  createdAt: ISODateTime;
}
