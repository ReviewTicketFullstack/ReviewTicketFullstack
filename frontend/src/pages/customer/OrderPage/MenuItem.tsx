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
      className={`w-full flex gap-4 p-4 border-2 first:rounded-t-2xl last:rounded-b-2xl transition-colors text-left ${
        isSelected ? "border-brand-800 bg-brand-50" : "border-transparent hover:bg-gray-50 active:bg-gray-100"
      }`}
      aria-pressed={isSelected}
    >
      {/* Image Section */}
      <div className="flex-shrink-0">
        {menu.imageUrl ? (
          <img
            src={menu.imageUrl}
            alt={menu.name}
            className="w-20 h-20 rounded-lg object-cover"
          />
        ) : (
          <div className="w-20 h-20 rounded-lg bg-gray-200 flex items-center justify-center">
            <span className="text-gray-400 text-xs">No Image</span>
          </div>
        )}
      </div>

      {/* Info Section */}
      <div className="flex-1 flex flex-col justify-center gap-1">
        {/* Name + Review Badge */}
        <div className="flex items-center gap-2">
          <span className="font-bold text-base">{menu.name}</span>
          {menu.reviewEvent && (
            <span className="inline-block bg-red-700 text-white text-xs font-semibold px-2 py-0.5 rounded">
              리뷰
            </span>
          )}
        </div>

        {/* Price */}
        <span className="text-sm text-gray-600">
          {menu.price.toLocaleString("ko-KR")}원
        </span>
      </div>
    </button>
  );
}
