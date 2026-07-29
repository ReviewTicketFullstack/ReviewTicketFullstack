import { useState } from 'react';
import { Button } from '@/shared/ui';
import { DetailStoreCard } from './DetailStoreCard';
import { MenuListCard, type MenuItemData } from './MenuListCard';

const MENU_DATA: MenuItemData[] = [
  { name: '피자', price: '18,000원', reviewBadge: true },
  { name: '햄버거', price: '9,000원', reviewBadge: true },
  { name: '치킨윙', price: '15,000원', reviewBadge: false },
  { name: '비빔밥', price: '10,000원', reviewBadge: false },
  { name: '라멘', price: '11,000원', reviewBadge: false },
];

export function OrderPage() {
  const [selectedMenu, setSelectedMenu] = useState<MenuItemData | null>(null);

  const handleMenuClick = (menu: MenuItemData) => {
    setSelectedMenu(menu);
  };

  return (
    <div className="space-y-6 px-5 py-6">
      <div>
        <h1 className="text-2xl font-bold">메뉴</h1>
        <p className="text-gray-600">메뉴확인</p>
      </div>

      {/* Store Promotion Card Section */}
      <DetailStoreCard
        storeName="도미너피자"
        rating={4.7}
        reviewCount="150"
      />

      {/* Menu List Section */}
      <MenuListCard menus={MENU_DATA} onMenuClick={handleMenuClick} />

      {/* Order Button Section */}
      <Button
        variant="primary"
        size="xlarge"
        fullWidth
        disabled={!selectedMenu}
      >
        {selectedMenu ? `${selectedMenu.price} 주문하기` : '메뉴를 선택해주세요'}
      </Button>
    </div>
  );
}
