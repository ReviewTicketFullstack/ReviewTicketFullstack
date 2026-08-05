/**
 * [TEMPORARY] 메뉴관리 페이지 목데이터.
 * FE-2.2 스펙의 음식 5종(피자, 햄버거, 치킨윙, 비빔밥, 라멘) 카탈로그 고정.
 * 백엔드 메뉴 API 나오면 이 파일만 훅으로 교체.
 */

export interface MenuListItem {
  id: string;
  name: string;
  price: number;
  hasReviewEvent: boolean;
}

export const menuItems: MenuListItem[] = [
  { id: 'menu-pizza', name: '피자', price: 18000, hasReviewEvent: true },
  { id: 'menu-burger', name: '햄버거', price: 9000, hasReviewEvent: true },
  { id: 'menu-chicken-wing', name: '치킨윙', price: 15000, hasReviewEvent: false },
  { id: 'menu-bibimbap', name: '비빔밥', price: 10000, hasReviewEvent: false },
  { id: 'menu-ramen', name: '라멘', price: 11000, hasReviewEvent: false },
];
