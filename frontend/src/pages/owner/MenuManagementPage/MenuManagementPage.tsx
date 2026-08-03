import { Card } from '@/shared/ui';
import { MenuListItem } from './MenuListItem';
import { menuItems } from './mockData';

export function MenuManagementPage() {
  return (
    <div className="flex flex-col gap-4 p-6">
      <div className="flex justify-end">
        <button
          type="button"
          disabled
          className="cursor-not-allowed rounded-lg bg-neutral-300 px-4 py-2 font-semibold text-neutral-500"
        >
          메뉴 추가
        </button>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="divide-y divide-gray-200">
          {menuItems.map((menu) => (
            <MenuListItem key={menu.id} menu={menu} />
          ))}
        </div>
      </Card>
    </div>
  );
}
