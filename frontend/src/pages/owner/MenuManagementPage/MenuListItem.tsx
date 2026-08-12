import type { MenuItem } from '@/api/storeApi';

interface MenuListItemProps {
  menu: MenuItem;
  onClick?: () => void;
}

export function MenuListItem({ menu, onClick }: MenuListItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full gap-4 p-4 text-left hover:bg-neutral-50"
    >
      {/* 메뉴 사진. 없는 메뉴는 사장이 아직 등록하지 않은 것이다.
          업로드는 줄을 눌러 여는 수정 모달에서 한다 — 이 줄 자체가 button 이라
          안에 또 button 을 넣을 수 없다. */}
      <div className="flex-shrink-0">
        {menu.imageUrl ? (
          <img
            src={menu.imageUrl}
            alt={menu.name}
            className="h-20 w-20 rounded-lg object-cover"
          />
        ) : (
          <span className="flex h-20 w-20 items-center justify-center rounded-lg bg-gray-200 text-xs text-neutral-400">
            사진 없음
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col justify-center gap-1">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold">{menu.name}</span>
          {/* 리뷰 이벤트 설정된 메뉴만 배지 표시 */}
          {menu.reviewEvent && (
            <span className="inline-block rounded bg-red-700 px-2 py-0.5 text-xs font-semibold text-white">
              리뷰
            </span>
          )}
          {/* 리뷰이벤트인데 기준 사진이 없으면 손님이 리뷰를 올리지 못한다 */}
          {menu.reviewEvent && !menu.imageUrl && (
            <span className="inline-block rounded bg-neutral-200 px-2 py-0.5 text-xs font-semibold text-neutral-600">
              사진 필요
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
