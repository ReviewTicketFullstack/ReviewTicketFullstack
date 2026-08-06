import { request } from "@/shared/api";

export interface StoreSummary {
  id: number;
  name: string;
  imageUrl: string | null;
  rating: number;
  reviewCount: number;
  hasReviewEvent: boolean;
}

export interface MenuItem {
  id: number;
  name: string;
  price: number;
  imageUrl: string | null;
  reviewEvent: boolean;
}

export interface StoreDetail {
  id: number;
  name: string;
  imageUrl: string | null;
  rating: number;
  reviewCount: number;
  menus: MenuItem[];
}

export function getStores(signal?: AbortSignal): Promise<StoreSummary[]> {
  return request<StoreSummary[]>("/stores", { auth: true, signal });
}

export function getStoreDetail(storeId: number, signal?: AbortSignal): Promise<StoreDetail> {
  return request<StoreDetail>(`/stores/${storeId}`, { auth: true, signal });
}
