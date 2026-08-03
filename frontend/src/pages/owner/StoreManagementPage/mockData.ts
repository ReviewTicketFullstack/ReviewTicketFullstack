/**
 * [TEMPORARY] 가게관리 페이지 목데이터.
 * 백엔드 가게 정보 API 나오면 이 파일만 훅으로 교체.
 */

export interface StoreInfo {
  name: string;
  address: string;
}

export const store: StoreInfo = {
  name: '도미너피자',
  address: '서울특별시 강남구 강남대로 396 강남역',
};
