/**
 * [TEMPORARY] 리뷰 관리 페이지 목데이터.
 * 백엔드 리뷰 API 나오면 이 파일만 훅으로 교체.
 */

export interface CompletedReview {
  id: string;
  reviewerId: string;
  /** 1-5 */
  rating: number;
  comment: string;
  photo?: string;
  /** ISO date string */
  createdAt: string;
}

export interface PendingOrder {
  id: string;
  ordererId: string;
  menuName: string;
  price: number;
  /** ISO date string */
  orderedAt: string;
}

export const completedReviews: CompletedReview[] = [
  {
    id: 'rv-1',
    reviewerId: 'user_minji',
    rating: 5,
    comment: '치즈버거 진짜 맛있어요! 재주문 의사 있습니다.',
    photo: '/mock/review-1.jpg',
    createdAt: '2026-08-01T10:12:00.000Z',
  },
  {
    id: 'rv-2',
    reviewerId: 'user_seongwon',
    rating: 4,
    comment: '토마토 베이컨 버거 소스가 특이하고 좋았어요.',
    photo: '/mock/review-2.jpg',
    createdAt: '2026-07-30T14:40:00.000Z',
  },
  {
    id: 'rv-3',
    reviewerId: 'user_doyeon',
    rating: 3,
    comment: '양이 조금 적은 느낌이었어요.',
    createdAt: '2026-07-28T09:05:00.000Z',
  },
];

export const pendingOrders: PendingOrder[] = [
  {
    id: 'od-1',
    ordererId: 'user_yujin',
    menuName: '치즈버거',
    price: 10000,
    orderedAt: '2026-08-02T11:20:00.000Z',
  },
  {
    id: 'od-2',
    ordererId: 'user_giwon',
    menuName: '토마토 베이컨 버거',
    price: 10000,
    orderedAt: '2026-08-01T18:30:00.000Z',
  },
];
