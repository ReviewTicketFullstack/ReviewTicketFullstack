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
  menuName: string;
  price: number;
}

export interface PendingOrder {
  id: string;
  ordererId: string;
  menuName: string;
  price: number;
  /** ISO date string */
  createdAt: string;
}

export const completedReviews: CompletedReview[] = [
  {
    id: 'rv-1',
    reviewerId: 'user_sample1',
    rating: 5,
    comment: 'sample피자 진짜 맛있어요! 재주문 의사 있습니다.',
    photo: '/mock/review-1.jpg',
    createdAt: '2026-08-01T10:12:00.000Z',
    menuName: 'sample피자',
    price: 18000,
  },
  {
    id: 'rv-2',
    reviewerId: 'user_sample2',
    rating: 4,
    comment: 'sample햄버거 정말 맛있었어요! 다음에 또 시킬게요.',
    photo: '/mock/review-2.jpg',
    createdAt: '2026-07-30T14:40:00.000Z',
    menuName: 'sample햄버거',
    price: 9000,
  },
  {
    id: 'rv-3',
    reviewerId: 'user_sample3',
    rating: 3,
    comment: 'sample양이 조금 적은 느낌이었어요.',
    createdAt: '2026-07-28T09:05:00.000Z',
    menuName: 'sample라멘',
    price: 11000,
  },
];

export const pendingOrders: PendingOrder[] = [
  {
    id: 'od-1',
    ordererId: 'user_sample4',
    menuName: 'sample치킨윙',
    price: 15000,
    createdAt: '2026-08-02T11:20:00.000Z',
  },
  {
    id: 'od-2',
    ordererId: 'user_sample5',
    menuName: 'sample비빔밥',
    price: 10000,
    createdAt: '2026-08-01T18:30:00.000Z',
  },
];
