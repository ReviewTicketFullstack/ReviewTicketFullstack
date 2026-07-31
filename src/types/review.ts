/**
 * [PROTOTYPE] 초기 설계 단계의 모델.
 * 현재 사용 중인 모델은 @/shared/types/review.ts 를 참조하세요.
 *
 * 주요 차이: photo 필드 미포함
 */
export type ReviewPassStatus = 'pass' | 'non-pass' ;

export interface Review {
  id: string;
  storeId: string;
  orderId?: string;
  /** 1-5 */
  rating: number;
  comment?: string;
  /** ISO date string. */
  createdAt: string;
  passStatus: ReviewPassStatus
}
