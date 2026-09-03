import { request } from "@/shared/api";
import type { ID, Order } from "@/entities/order";

/**
 * 주문 생성 응답. 목록(Order)과 같은 필드에 tickets 하나가 더 붙는다 —
 * 잠금이 반영된 뒤의 잔여 티켓이라 상단 바 뱃지를 이 값으로 바로 갱신할 수 있다.
 */
export interface OrderCreated extends Order {
  tickets: number;
}

/**
 * 주문 생성.
 *
 * 가격을 보내지 않는다 — 서버가 menuId 로 조회해 담는다.
 * 프론트가 보낸 금액을 그대로 믿으면 개발자도구로 고쳐 보낼 수 있기 때문이다.
 * 주문자도 보내지 않는다. 토큰의 주체가 곧 주문자다.
 */
export function createOrder(
  storeId: ID,
  menuId: ID,
  reviewEventApply: boolean,
): Promise<OrderCreated> {
  return request<OrderCreated>("/orders", {
    method: "POST",
    body: { storeId, menuId, reviewEventApply },
    auth: true,
  });
}

/** 로그인한 본인 주문만, 최신순. */
export function getMyOrders(signal?: AbortSignal): Promise<Order[]> {
  return request<Order[]>("/orders", { auth: true, signal });
}
