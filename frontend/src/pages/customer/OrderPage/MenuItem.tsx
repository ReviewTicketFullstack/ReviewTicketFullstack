import { UtensilsCrossed } from "lucide-react";
import { Badge } from "@/shared/ui";

export interface MenuItemData {
  id: number;
  name: string;
  /** 원 단위 정수. 표시 형식은 이 컴포넌트가 맡는다 */
  price: number;
  reviewEvent: boolean;
  imageUrl: string | null;
}

export interface MenuItemProps {
  menu: MenuItemData;
  onClick: (menu: MenuItemData) => void;
  isSelected: boolean;
}

export function MenuItem({ menu, onClick, isSelected }: MenuItemProps) {
  return (
    <button
      type="button"
      onClick={() => onClick(menu)}
      // button 안에는 phrasing content 만 넣는다(design.md Card).
      // 선택 상태는 색 외에 테두리로도 드러나야 한다.
      className={`flex w-full gap-3 border-2 p-3 text-left transition-colors ${
        isSelected
          ? "border-brand-800 bg-brand-50"
          : "border-transparent hover:bg-fill-100 active:bg-line-100"
      }`}
      aria-pressed={isSelected}
    >
      {/* Image Section */}
      <span className="flex size-20 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-fill-100">
        {menu.imageUrl ? (
          <img
            src={menu.imageUrl}
            alt=""
            className="h-full w-full object-cover"
          />
        ) : (
          <UtensilsCrossed size={22} className="text-ink-300" aria-hidden="true" />
        )}
      </span>

      {/* Info Section */}
      <span className="flex min-w-0 flex-1 flex-col justify-center gap-1">
        <span className="flex items-center gap-2">
          <span className="min-w-0 truncate text-base font-bold text-ink-900">
            {menu.name}
          </span>
          {menu.reviewEvent && (
            <Badge variant="accent" className="shrink-0">
              리뷰
            </Badge>
          )}
        </span>

        <span className="text-sm text-ink-700">
          {menu.price.toLocaleString("ko-KR")}원
        </span>
      </span>
    </button>
  );
}
