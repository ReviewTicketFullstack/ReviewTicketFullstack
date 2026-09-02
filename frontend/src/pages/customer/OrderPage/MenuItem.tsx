export interface MenuItemData {
  name: string;
  price: string;
  reviewBadge: boolean;
}

export interface MenuItemProps {
  menu: MenuItemData;
  onClick: (menu: MenuItemData) => void;
}

export function MenuItem({ menu, onClick }: MenuItemProps) {
  return (
    <button
      type="button"
      onClick={() => onClick(menu)}
      className="w-full flex gap-4 p-4 hover:bg-gray-50 active:bg-gray-100 transition-colors text-left"
    >
      {/* Image Section */}
      <div className="flex-shrink-0">
        <div className="w-20 h-20 rounded-lg bg-gray-200 flex items-center justify-center">
          <span className="text-gray-400 text-xs">Image</span>
        </div>
      </div>

      {/* Info Section */}
      <div className="flex-1 flex flex-col justify-center gap-1">
        {/* Name + Review Badge */}
        <div className="flex items-center gap-2">
          <span className="font-bold text-base">{menu.name}</span>
          {menu.reviewBadge && (
            <span className="inline-block bg-red-700 text-white text-xs font-semibold px-2 py-0.5 rounded">
              리뷰
            </span>
          )}
        </div>

        {/* Price */}
        <span className="text-sm text-gray-600">{menu.price}</span>
      </div>
    </button>
  );
}
