import { request } from "@/shared/api";
import type { Store } from "@/entities/store";

export interface MenuItem {
  id: number;
  name: string;
  price: number;
  imageUrl: string;
  reviewEvent: boolean;
}

export interface StoreDetail extends Store {
  menus: MenuItem[];
}

/**
 * [TODO] GET /api/stores 명세 미확정. 아래 두 가지가 확정되면 맞춘다.
 *  - 응답이 배열 그대로인지 { stores: [...] } 래핑인지
 *  - 페이지네이션 파라미터가 붙는지
 */
export function getStores(signal?: AbortSignal): Promise<Store[]> {
  return request<Store[]>("/stores", { auth: true, signal });
}

/** 가게 상세 정보와 메뉴 목록 */
export function getStoreDetail(storeId: number, signal?: AbortSignal): Promise<StoreDetail> {
  return request<StoreDetail>(`/stores/${storeId}`, { auth: true, signal });
}
