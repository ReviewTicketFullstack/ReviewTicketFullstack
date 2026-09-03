import { request } from "@/shared/api";
import type { ID, ISODateTime } from "@/entities/order";

/** POST /api/reviews 성공 응답. tickets 는 반환이 반영된 뒤의 잔여 티켓이다. */
export interface ReviewCreated {
  reviewId: ID;
  orderId: ID;
  storeId: ID;
  menuId: ID;
  userId: ID;
  reviewRating: number;
  reviewContent: string;
  reviewImageUrl: string;
  reviewCreatedAt: ISODateTime;
  /** AI 가 매긴 유사도. 0.80 이상만 저장되므로 항상 그 이상이다 */
  imageSimilarity: number;
  /** 판정 기준이 된 메뉴 표본 사진 */
  compareImageUrl: string;
  tickets: number;
}

/** 사장의 리뷰관리 화면, 리뷰완료 탭 항목. 자기 가게 리뷰라 판정 근거까지 실린다. */
export interface OwnerReview {
  reviewId: ID;
  orderId: ID;
  menuId: ID;
  menuName: string;
  /** 주문 시점의 가격 */
  menuPrice: number;
  /** 리뷰를 쓴 손님의 닉네임 */
  displayName: string;
  reviewRating: number;
  reviewContent: string;
  reviewImageUrl: string;
  reviewCreatedAt: ISODateTime;
  imageSimilarity: number;
  compareImageUrl: string;
}

/** 상단 통계를 목록과 함께 받는다 — 프론트가 배열을 훑어 평균을 내면 페이지네이션에서 틀어진다. */
export interface OwnerReviewList {
  reviewNumber: number;
  reviewValue: number;
  reviews: OwnerReview[];
}

/** 사장의 리뷰관리 화면, 리뷰미작성 탭 항목. */
export interface PendingOrder {
  orderId: ID;
  /** 주문한 손님의 닉네임 */
  displayName: string;
  menuId: ID;
  menuName: string;
  menuPrice: number;
  orderedAt: ISODateTime;
  expireTime: ISODateTime;
  /** pending: 기한 남음(리뷰미작성) / expired: 기한 지남(미이행) */
  reviewStatus: "pending" | "expired";
}

/**
 * 리뷰 제출. 사진이 함께 가므로 multipart 로 보낸다.
 *
 * storeId, menuId, userId 는 보내지 않는다 — orderId 로 주문을 찾으면 셋 다
 * 따라 나오고, 프론트가 보낸 값을 믿으면 남의 가게에 리뷰가 달릴 수 있다.
 *
 * 성공해야 비로소 리뷰가 존재한다. 사진이 메뉴 표본과 다르면 422
 * IMAGE_NOT_MATCHED 가 오고 아무것도 저장되지 않는다 — 티켓은 잠긴 채 남아
 * 기한 안이라면 다시 시도할 수 있다.
 */
export function createReview(
  orderId: ID,
  reviewRating: number,
  reviewContent: string,
  image: File,
): Promise<ReviewCreated> {
  const form = new FormData();
  form.append("orderId", String(orderId));
  form.append("reviewRating", String(reviewRating));
  form.append("reviewContent", reviewContent);
  form.append("image", image);

  return request<ReviewCreated>("/reviews", {
    method: "POST",
    body: form,
    auth: true,
  });
}

/** 그 가게에 달린 리뷰 전부. 누가 썼는지 가리지 않는다. 모르는 손님도 보는 화면이라
 * userId, orderId, imageSimilarity, compareImageUrl 은 실리지 않는다. */
export interface PublicReview {
  reviewId: ID;
  menuId: ID;
  menuName: string;
  displayName: string;
  reviewRating: number;
  reviewContent: string;
  reviewImageUrl: string;
  reviewCreatedAt: ISODateTime;
}

export function getStoreReviews(
  storeId: ID,
  signal?: AbortSignal,
): Promise<PublicReview[]> {
  return request<PublicReview[]>(`/stores/${storeId}/reviews`, {
    auth: true,
    signal,
  });
}

/** 사장 본인 가게의 리뷰 목록과 통계. */
export function getMyStoreReviews(
  signal?: AbortSignal,
): Promise<OwnerReviewList> {
  return request<OwnerReviewList>("/stores/me/reviews", { auth: true, signal });
}

/** 리뷰이벤트에 참여했으나 아직 리뷰가 없는 주문 목록. */
export function getMyPendingOrders(
  signal?: AbortSignal,
): Promise<PendingOrder[]> {
  return request<PendingOrder[]>("/stores/me/orders/pending", {
    auth: true,
    signal,
  });
}
