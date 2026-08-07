import { useEffect, useState } from 'react';
import { Card } from '@/shared/ui';
import { useAuth } from '@/app/providers';
import { MenuListItem } from './MenuListItem';
import { MenuEditModal } from './MenuEditModal';
import { getMyMenus, type MenuItem } from '@/api/storeApi';

export function MenuManagementPage() {
  const { user } = useAuth();
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  // 클릭한 메뉴 — 있으면 수정 모달이 열림
  const [selectedMenu, setSelectedMenu] = useState<MenuItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 내 가게 메뉴 조회. 가게 번호를 보내지 않는다 — 토큰의 주체가 곧 그 가게의 사장이다.
  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      try {
        const menus = await getMyMenus(controller.signal);
        // 사장용 응답은 필드명 앞에 menu 가 붙고 시각까지 실려 온다.
        // 목록 컴포넌트가 쓰는 형태로 옮겨 담는다.
        setMenuItems(
          menus.map((menu) => ({
            id: menu.menuId,
            name: menu.menuName,
            price: menu.menuPrice,
            imageUrl: menu.menuImageUrl,
            reviewEvent: menu.reviewEvent,
          })),
        );
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setError('메뉴를 불러오지 못했습니다.');
      } finally {
        setIsLoading(false);
      }
    }

    load();
    return () => controller.abort();
  }, [user?.displayName]);

  // 모달의 "적용" 클릭 시: 로컬 state만 갱신 — 메뉴 수정 저장 API는 아직 없음
  const handleApply = (reviewEvent: boolean) => {
    if (!selectedMenu) return;
    setMenuItems((items) =>
      items.map((item) => (item.id === selectedMenu.id ? { ...item, reviewEvent } : item))
    );
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

      {isLoading && <p className="text-sm text-neutral-500">불러오는 중...</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}

      {!isLoading && !error && (
        <Card className="overflow-hidden p-0">
          <div className="divide-y divide-gray-200">
            {menuItems.map((menu) => (
              <MenuListItem key={menu.id} menu={menu} onClick={() => setSelectedMenu(menu)} />
            ))}
          </div>
        </Card>
      )}

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
