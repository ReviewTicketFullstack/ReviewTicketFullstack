/**
 * [PROTOTYPE] 초기 설계 단계의 모델.
 * 현재 사용 중인 모델은 @/shared/types/store.ts 를 참조하세요.
 */
export interface Store {
  id: string;
  name: string;
  description?: string;
  imageUrl?: string;
  countReview: string;
  reviewBadge?: string;
}