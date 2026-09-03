import { Card } from '@/shared/ui';
import { MenuItem, type MenuItemData } from './MenuItem';

export type { MenuItemData };

export interface MenuListCardProps {
  menus: MenuItemData[];
  onMenuClick: (menu: MenuItemData) => void;
  /** 선택된 메뉴. 없으면 null */
  selectedMenuId: number | null;
}

export function MenuListCard({ menus, onMenuClick, selectedMenuId }: MenuListCardProps) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="divide-y divide-line-100">
        {menus.map((menu) => (
          <MenuItem
            key={menu.id}
            menu={menu}
            onClick={onMenuClick}
            isSelected={menu.id === selectedMenuId}
          />
        ))}
      </div>
    </Card>
  );
}
