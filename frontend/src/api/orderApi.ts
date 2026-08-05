import { request } from "@/shared/api";
import type { ID, Order } from "@/entities/order";

/**
 * 주문 생성.
 *
 * 가격을 보내지 않는다 — 서버가 menuId 로 조회해 담는다.
 * 프론트가 보낸 금액을 그대로 믿으면 개발자도구로 고쳐 보낼 수 있기 때문이다.
 * 주문자도 보내지 않는다. 토큰의 주체가 곧 주문자다.
 */
export function createOrder(storeId: ID, menuId: ID): Promise<Order> {
  return request<Order>("/orders", {
    method: "POST",
    body: { storeId, menuId },
    auth: true,
  });
}

/** 로그인한 본인 주문만, 최신순. */
export function getMyOrders(signal?: AbortSignal): Promise<Order[]> {
  return request<Order[]>("/orders", { auth: true, signal });
}
