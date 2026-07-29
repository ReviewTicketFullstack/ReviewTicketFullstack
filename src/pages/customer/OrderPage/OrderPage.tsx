import { useState } from 'react';
import { Button } from '@/shared/ui';
import { DetailStoreCard } from './DetailStoreCard';
import { MenuListCard, type MenuItemData } from './MenuListCard';
import { useParams } from "react-router-dom";
import { saveOrder } from '@/features/order/orderStorage';

const MENU_DATA: MenuItemData[] = [
  { name: '피자', price: '18,000원', reviewBadge: true },
  { name: '햄버거', price: '9,000원', reviewBadge: true },
  { name: '치킨윙', price: '15,000원', reviewBadge: false },
  { name: '비빔밥', price: '10,000원', reviewBadge: false },
  { name: '라멘', price: '11,000원', reviewBadge: false },
];

const parsePrice = (priceStr: string): number => {
  return parseInt(priceStr.replace(/[^0-9]/g, ''), 10);
};

export function OrderPage() {
  const { storeId } = useParams();

  const [selectedMenu, setSelectedMenu] = useState<MenuItemData | null>(null);

  const handleMenuClick = (menu: MenuItemData) => {
    setSelectedMenu(menu);
  };

  const handleOrderClick = () => {
    if (!selectedMenu || !storeId) return;

    saveOrder({
      storeId,
      storeName: '도미너피자',
      menuName: selectedMenu.name,
      price: parsePrice(selectedMenu.price),
      hasReviewBadge: selectedMenu.reviewBadge,
    });

    setSelectedMenu(null);
    alert('주문이 완료되었습니다!');
  };

  return (
    <div className="space-y-6 px-5 py-6">
    
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
        onClick={handleOrderClick}
      >
        {selectedMenu ? `${selectedMenu.price} 주문하기` : '메뉴를 선택해주세요'}
      </Button>
    </div>
  );
}
