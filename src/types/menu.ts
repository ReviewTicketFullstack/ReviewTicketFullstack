/**
 * [PROTOTYPE] 초기 설계 단계의 모델.
 * 현재 사용 중인 모델은 @/shared/types/menu.ts 를 참조하세요.
 */
export interface MenuItem {
  id: string;
  storeId: string;
  name: string;
  price: number;
  imageUrl?: string;
  reviewBadge?: string;
}
