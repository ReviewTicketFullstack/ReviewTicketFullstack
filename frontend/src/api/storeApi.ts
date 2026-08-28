import { request } from "@/shared/api";
import type { Store } from "@/entities/store";

// 이도연
export interface MenuItem {
  id: number;
  name: string;
  price: number;
  /** 점주가 표본 사진을 등록하지 않은 메뉴는 null 이다 */
  imageUrl: string | null;
  reviewEvent: boolean;
}

export interface StoreDetail extends Store {
  menus: MenuItem[];
}

export function getStores(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<Store[]> {
  return request<Store[]>(`/stores?page=${page}&size=${size}`, {
    auth: true,
    signal,
  });
}

export function getStoreDetail(
  storeId: number,
  signal?: AbortSignal,
): Promise<StoreDetail> {
  return request<StoreDetail>(`/stores/${storeId}`, { auth: true, signal });
}

/**
 * 사장 본인 가게. 고객용 목록/상세와 필드명이 다르다 — 사장 화면은 생성·수정
 * 시각까지 쓰기 때문에 서버가 별도 응답으로 내려준다.
 */
export interface MyStore {
  storeId: number;
  ownerId: number;
  storeName: string;
  logoUrl: string | null;
  reviewNumber: number;
  reviewValue: number;
  isReviewing: boolean;
  createdAt: string;
  latestUpdate: string;
}

/** 사장 본인 가게의 메뉴. 고객용 MenuItem 과 달리 등록·수정 시각이 붙는다. */
export interface MyMenuItem {
  storeId: number;
  menuId: number;
  menuName: string;
  menuPrice: number;
  /** 목록·손님 화면에 뜨는 대표 사진 한 장. AI 대조에는 안 쓰인다 */
  menuImageUrl: string | null;
  /** AI 대조용 표본 사진 5칸. 순서 그대로, 빈 칸은 null */
  sampleImageUrls: (string | null)[];
  reviewEvent: boolean;
  menuCreatedAt: string;
  menuLatestUpdate: string;
}

/** PATCH /api/stores/me 응답. 방금 바뀐 값과 갱신 시각만 돌려준다. */
export interface UpdateStoreResponse {
  storeId: number;
  storeName: string;
  logoUrl: string | null;
  latestUpdate: string;
}

/** 가게 번호를 보내지 않는다 — 토큰의 주체가 곧 그 가게의 사장이다. */
export function getMyStore(signal?: AbortSignal): Promise<MyStore> {
  return request<MyStore>("/stores/me", { auth: true, signal });
}

/** 가게 이름과 로고를 통째로 덮어쓴다. logoUrl 이 null 이면 로고를 지운다는 뜻이다. */
export function updateMyStore(
  storeName: string,
  logoUrl: string | null,
): Promise<UpdateStoreResponse> {
  return request<UpdateStoreResponse>("/stores/me", {
    method: "PATCH",
    body: { storeName, logoUrl },
    auth: true,
  });
}
export function getMyMenus(signal?: AbortSignal): Promise<MyMenuItem[]> {
  return request<MyMenuItem[]>("/stores/me/menus", { auth: true, signal });
}

/** PATCH /api/stores/me/menus/{id} 응답. 방금 바뀐 값과 갱신 시각만 돌려준다. */
export interface UpdateMenuResponse {
  menuId: number;
  menuImageUrl: string | null;
  sampleImageUrls: (string | null)[];
  reviewEvent: boolean;
  menuLatestUpdate: string;
}

/**
 * 사장이 직접 메뉴를 추가한다.
 *
 * 엔드포인트 : POST /api/stores/me/menus (201 Created)
 * 요청 바디  : { menuName, menuPrice, imageUrl, sampleImageUrls, reviewEvent }
 * 응답       : MenuOwnerResponse — GET /me/menus 한 줄과 동일한 형태
 *
 * sampleImageUrls 는 최소 1장 이상이어야 한다 — 서버가 생성 시점에도
 * requireValidSamples 를 적용하며, 없으면 SAMPLE_IMAGE_REQUIRED 로 거절한다.
 */
export function createMyMenu(
  menuName: string,
  menuPrice: number,
  imageUrl: string | null,
  sampleImageUrls: (string | null)[],
  reviewEvent: boolean,
  signal?: AbortSignal,
): Promise<MyMenuItem> {
  return request<MyMenuItem>("/stores/me/menus", {
    method: "POST",
    body: { menuName, menuPrice, imageUrl, sampleImageUrls, reviewEvent },
    auth: true,
    signal,
  });
}

/**
 * 메뉴의 대표 사진·표본 사진·리뷰이벤트 여부를 통째로 덮어쓴다.
 *
 * sampleImageUrls 는 한 장 이상 값이 있어야 한다 — 서버가 SAMPLE_IMAGE_REQUIRED
 * 로 거절한다. 이름·가격은 이 요청으로 못 바꾼다(서버에 그 필드 자체가 없다).
 */
export function updateMyMenu(
  menuId: number,
  patch: {
    imageUrl: string | null;
    sampleImageUrls: (string | null)[];
    reviewEvent: boolean;
  },
): Promise<UpdateMenuResponse> {
  return request<UpdateMenuResponse>(`/stores/me/menus/${menuId}`, {
    method: "PATCH",
    body: patch,
    auth: true,
  });
}
