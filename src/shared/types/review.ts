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
