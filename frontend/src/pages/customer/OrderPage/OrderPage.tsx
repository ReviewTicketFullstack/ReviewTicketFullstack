import { useState } from "react";
import { Button } from "@/shared/ui";
import { DetailStoreCard } from "./DetailStoreCard";
import { MenuListCard, type MenuItemData } from "./MenuListCard";
import { useParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { createOrder } from "@/api/orderApi";
import { ApiError } from "@/shared/api";

/**
 * [TODO] 임시 메뉴 데이터.
 *
 * 가게 상세 API(GET /api/stores/{storeId}) 연동 시 통째로 교체된다 —
 * front-customer-store 브랜치 담당. 주문이 성공하려면 id 가 서버 menu 테이블
 * 값과 같아야 한다.
 */
const MENU_DATA: MenuItemData[] = [
  { id: 1, name: "피자", price: 18000, reviewEvent: true },
  { id: 2, name: "햄버거", price: 9000, reviewEvent: true },
  { id: 3, name: "치킨윙", price: 15000, reviewEvent: false },
  { id: 4, name: "비빔밥", price: 10000, reviewEvent: false },
  { id: 5, name: "라멘", price: 11000, reviewEvent: false },
];

export function OrderPage() {
  const { storeId } = useParams();
  const navigate = useNavigate();

  const [selectedMenu, setSelectedMenu] = useState<MenuItemData | null>(null);
  const [isOrdering, setIsOrdering] = useState(false);
  const [orderError, setOrderError] = useState("");

  const handleMenuClick = (menu: MenuItemData) => {
    setSelectedMenu(menu);
    setOrderError("");
  };

  const handleOrderClick = async () => {
    if (!selectedMenu || !storeId) return;

    setIsOrdering(true);
    setOrderError("");

    try {
      // 가격은 보내지 않는다. 서버가 menuId 로 조회해 담는다.
      await createOrder(Number(storeId), selectedMenu.id);
      setSelectedMenu(null);
      // replace 로 이동해 뒤로가기가 주문 화면으로 돌아오지 않게 한다.
      navigate("/order-history", { replace: true });
    } catch (error) {
      setOrderError(
        error instanceof ApiError ? error.message : "주문하지 못했습니다.",
      );
    } finally {
      setIsOrdering(false);
    }
  };

  return (
    <div className="space-y-6 pb-32">
      {/* Store Promotion Card Section */}
      <DetailStoreCard storeName="도미너피자" rating={4.7} reviewCount="150" />

      {/* Menu List Section */}
      <MenuListCard menus={MENU_DATA} onMenuClick={handleMenuClick} />

      {/* Order Button Section */}
      <div
        className="fixed
                  bottom-0
                  left-1/2
                  w-full
                  max-w-[860px]
                  -translate-x-1/2"
      >
        <div className="bg-gradient-to-t from-white via-white/90 to-transparent p-5">
          {orderError && (
            <p className="mb-2 text-center text-sm text-red-600">{orderError}</p>
          )}
          <Button
            variant="primary"
            size="xlarge"
            fullWidth
            disabled={!selectedMenu || isOrdering}
            onClick={handleOrderClick}
          >
            {isOrdering
              ? "주문 중..."
              : selectedMenu
                ? `${selectedMenu.price.toLocaleString("ko-KR")}원 주문하기`
                : "메뉴를 선택해주세요"}
          </Button>
        </div>
      </div>
    </div>
  );
}
