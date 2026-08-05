import type { MenuListItem as MenuListItemType } from './mockData';

interface MenuListItemProps {
  menu: MenuListItemType;
  onClick?: () => void;
}

export function MenuListItem({ menu, onClick }: MenuListItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full gap-4 p-4 text-left hover:bg-neutral-50"
    >
      {/* 메뉴 이미지 자리 — 실제 이미지 없어서 placeholder */}
      <div className="flex-shrink-0">
        <div className="flex h-20 w-20 items-center justify-center rounded-lg bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>
      </div>

      <div className="flex flex-1 flex-col justify-center gap-1">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold">{menu.name}</span>
          {/* 리뷰 이벤트 설정된 메뉴만 배지 표시 */}
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
    </button>
  );
}
