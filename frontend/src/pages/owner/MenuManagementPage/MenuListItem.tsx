import type { MenuListItem as MenuListItemType } from './mockData';

interface MenuListItemProps {
  menu: MenuListItemType;
}

export function MenuListItem({ menu }: MenuListItemProps) {
  return (
    <div className="flex w-full gap-4 p-4 text-left">
      <div className="flex-shrink-0">
        <div className="flex h-20 w-20 items-center justify-center rounded-lg bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>
      </div>

      <div className="flex flex-1 flex-col justify-center gap-1">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold">{menu.name}</span>
          {menu.hasReviewEvent && (
            <span className="inline-block rounded bg-red-700 px-2 py-0.5 text-xs font-semibold text-white">
              리뷰
            </span>
          )}
        </div>
        <span className="text-sm text-gray-600">
          {menu.price.toLocaleString('ko-KR')}원
        </span>
      </div>
    </div>
  );
}
