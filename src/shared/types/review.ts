/**
 * 현재 사용 중인 Review 모델.
 *
 * photo 필드는 Base64 encoded image data 를 담으려 설계되었습니다.
 * 향후 S3 presigned URL 방식으로 변경될 가능성이 있으니 주의하세요.
 */
export type ReviewPassStatus = 'pass' | 'non-pass';

export interface Review {
  id: string;
  storeId: string;
  orderId?: string;
  /** 1-5 */
  rating: number;
  comment?: string;
  /** Base64 encoded image data */
  photo?: string;
  /** ISO date string. */
  createdAt: string;
  passStatus: ReviewPassStatus;
}
