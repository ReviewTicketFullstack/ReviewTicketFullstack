import { ImageOff } from 'lucide-react';
import { Badge } from '@/shared/ui';
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
      className="flex w-full gap-3 p-3 text-left transition-colors hover:bg-fill-100 active:bg-line-100"
    >
      {/* 메뉴 사진. 없는 메뉴는 사장이 아직 등록하지 않은 것이다.
          업로드는 줄을 눌러 여는 수정 모달에서 한다 — 이 줄 자체가 button 이라
          안에 또 button 을 넣을 수 없다. */}
      <span className="flex size-20 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-fill-100">
        {menu.imageUrl ? (
          <img src={menu.imageUrl} alt="" className="h-full w-full object-cover" />
        ) : (
          <ImageOff size={22} className="text-ink-300" aria-hidden="true" />
        )}
      </span>

      <span className="flex min-w-0 flex-1 flex-col justify-center gap-1">
        <span className="flex flex-wrap items-center gap-2">
          <span className="min-w-0 truncate text-base font-bold text-ink-900">
            {menu.name}
          </span>
          {/* 리뷰 이벤트 설정된 메뉴만 배지 표시 */}
          {menu.reviewEvent && <Badge variant="accent">리뷰</Badge>}
          {/* 리뷰이벤트인데 기준 사진이 없으면 손님이 리뷰를 올리지 못한다 */}
          {menu.reviewEvent && !menu.imageUrl && (
            <Badge variant="info">사진 필요</Badge>
          )}
        </span>
        <span className="text-sm text-ink-700">
          {menu.price.toLocaleString('ko-KR')}원
        </span>
      </span>
    </button>
  );
}
