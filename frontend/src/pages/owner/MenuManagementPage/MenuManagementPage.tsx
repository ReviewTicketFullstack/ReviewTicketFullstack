import { useState } from 'react';
import { Card } from '@/shared/ui';
import { MenuListItem } from './MenuListItem';
import { MenuEditModal } from './MenuEditModal';
import { menuItems as sharedMenuItems, type MenuListItem as MenuListItemType } from './mockData';

export function MenuManagementPage() {
  const [menuItems, setMenuItems] = useState(sharedMenuItems);
  // 클릭한 메뉴 — 있으면 수정 모달이 열림
  const [selectedMenu, setSelectedMenu] = useState<MenuListItemType | null>(null);

  // 모달의 "적용" 클릭 시: 선택된 메뉴의 리뷰이벤트 여부만 갱신하고 모달 닫기
  const handleApply = (hasReviewEvent: boolean) => {
    if (!selectedMenu) return;
    const updated = menuItems.map((item) =>
      item.id === selectedMenu.id ? { ...item, hasReviewEvent } : item
    );
    setMenuItems(updated);
    // mockData 원본 배열도 갱신 — 다른 페이지 갔다 와도(재mount) 유지되게
    sharedMenuItems.splice(0, sharedMenuItems.length, ...updated);
    setSelectedMenu(null);
  };

  return (
    <div className="flex flex-col gap-4 p-6">
      <div className="flex flex-col gap-2">
        <div className="flex justify-between items-center">
          <h1 className="text-xl font-bold text-ink-900">메뉴관리</h1>
        </div>
        <div className="flex justify-end">
          {/* 메뉴 추가 기능 아직 미구현 — 자리만 잡아둔 비활성 버튼 */}
          <button
            type="button"
            disabled
            className="cursor-not-allowed rounded-lg bg-neutral-300 px-4 py-2 font-semibold text-neutral-500"
          >
            메뉴 추가
          </button>
        </div>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="divide-y divide-gray-200">
          {menuItems.map((menu) => (
            <MenuListItem key={menu.id} menu={menu} onClick={() => setSelectedMenu(menu)} />
          ))}
        </div>
      </Card>

      {/* 메뉴 클릭 시에만 수정 모달 표시 */}
      {selectedMenu && (
        <MenuEditModal
          menu={selectedMenu}
          onClose={() => setSelectedMenu(null)}
          onApply={handleApply}
        />
      )}
    </div>
  );
}
