import { request } from "@/shared/api";
import type { Store } from "@/entities/store";

// 강성원 코드
// export interface StoreSummary {
//   id: number;
//   name: string;
//   imageUrl: string | null;
//   rating: number;
//   reviewCount: number;
//   hasReviewEvent: boolean;
// }

// 이도연
export interface MenuItem {
  id: number;
  name: string;
  price: number;
  imageUrl: string | null;
  reviewEvent: boolean;
}

// 이도연 코드
export interface StoreDetail extends Store {
  menus: MenuItem[];
}

// 강성원 코드
// export interface StoreDetail {
//   id: number;
//   name: string;
//   imageUrl: string | null;
//   rating: number;
//   reviewCount: number;
//   menus: MenuItem[];
// }

// 이도연
export function getStores(signal?: AbortSignal): Promise<Store[]> {
  return request<Store[]>("/stores", { auth: true, signal });
}

// 이도연
export function getStoreDetail(
  storeId: number,
  signal?: AbortSignal,
): Promise<StoreDetail> {
  return request<StoreDetail>(`/stores/${storeId}`, { auth: true, signal });
}
