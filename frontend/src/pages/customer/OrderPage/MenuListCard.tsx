import { Card } from '@/shared/ui';
import { MenuItem, type MenuItemData } from './MenuItem';

export type { MenuItemData };

export interface MenuListCardProps {
  menus: MenuItemData[];
  onMenuClick: (menu: MenuItemData) => void;
}

export function MenuListCard({ menus, onMenuClick }: MenuListCardProps) {
  return (
    <Card className="p-0 overflow-hidden">
      <div className="divide-y divide-gray-200">
        {menus.map((menu) => (
          <MenuItem
            key={menu.id}
            menu={menu}
            onClick={onMenuClick}
          />
        ))}
      </div>
    </Card>
  );
}
