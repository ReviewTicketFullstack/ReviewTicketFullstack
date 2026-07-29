export interface Order {
  id: string;
  storeId: string;
  storeName: string;
  menuName: string;
  price: number;
  hasReviewBadge: boolean;
  createdAt: string;
  reviewDeadline: string;
}
