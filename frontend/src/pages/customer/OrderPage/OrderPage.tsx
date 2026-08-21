import { useEffect, useState } from "react";
import { Button } from "@/shared/ui";
import { DetailStoreCard } from "./DetailStoreCard";
import { MenuListCard, type MenuItemData } from "./MenuListCard";
import { StoreReviewSection } from "./StoreReviewSection";
import { useParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { createOrder } from "@/api/orderApi";
import { getStoreDetail, type StoreDetail } from "@/api/storeApi";
import { saveOrder } from "@/entities/order/orderStorage";
import { clearStoreCache } from "@/entities/store";
import { ApiError } from "@/shared/api";
import { useAuth } from "@/app/providers";
import { ChevronLeft } from "lucide-react";

export function OrderPage() {
  const { storeId } = useParams();
  const { updateTickets } = useAuth();
  const navigate = useNavigate();

  const [storeDetail, setStoreDetail] = useState<StoreDetail | null>(null);
  const [isLoadingStore, setIsLoadingStore] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [selectedMenu, setSelectedMenu] = useState<MenuItemData | null>(null);
  const [isOrdering, setIsOrdering] = useState(false);
  const [orderError, setOrderError] = useState("");

  useEffect(() => {
    if (!storeId) return;

    setIsLoadingStore(true);
    setLoadError("");

    getStoreDetail(Number(storeId))
      .then((data) => {
        setStoreDetail(data);
      })
      .catch((error) => {
        // 홈 목록 캐시가 가리키는 번호가 실제로는 없는 경우다 — 캐시가 만들어진
        // 뒤 서버 데이터가 재구성됐을 때 생긴다. 캐시를 지워 다음 홈 진입 때
        // 새로 받아오게 한다. 이 화면 자체는 안내만 하고 홈으로 보낸다.
        if (error instanceof ApiError && error.status === 404) {
          clearStoreCache();
        }
        setLoadError(
          error instanceof ApiError
            ? error.message
            : "가게 정보를 불러오지 못했습니다.",
        );
      })
      .finally(() => {
        setIsLoadingStore(false);
      });
  }, [storeId]);

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
      // 리뷰이벤트 대상 메뉴면 참여 신청까지 함께 보낸다 — 이 값이 true 여야
      // 서버가 티켓을 잠그고 마감 시각을 만든다. 참여 여부를 따로 고르는
      // 화면이 생기면 그때 그 선택값으로 바꾼다.
      const order = await createOrder(
        Number(storeId),
        selectedMenu.id,
        selectedMenu.reviewEvent,
      );
      // 서버가 준 주문을 그대로 사본에 남긴다. 서버에 닿지 못할 때 쓴다.
      saveOrder(order);
      // 서버가 잠금 반영 후의 잔여 티켓을 함께 준다. - 추가 조회 없이 상단 뱃지를 맞춘다.
      updateTickets(order.tickets);
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

  if (isLoadingStore) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-600">가게 정보를 불러오는 중...</p>
      </div>
    );
  }

  if (!storeDetail || loadError) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
        <p className="text-red-600">
          {loadError || "가게를 찾을 수 없습니다."}
        </p>
        <Button variant="secondary" onClick={() => navigate("/")}>
          돌아가기
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-32 px-5">
      {/* Back Button Section */}
      <button
        type="button"
        onClick={() => navigate("/")}
        aria-label="가게 목록으로 돌아가기"
        className="-ml-3 flex size-11 items-center justify-center rounded-lg text-ink-900 hover:bg-fill-100 active:bg-line-100"
      >
        <ChevronLeft size={24} />
      </button>

      {/* Store Promotion Card Section */}
      <DetailStoreCard
        storeName={storeDetail.name}
        rating={storeDetail.rating}
        reviewCount={String(storeDetail.reviewCount)}
        hasReviewEvent={storeDetail.hasReviewEvent}
      />

      {/* Menu List Section */}
      <MenuListCard
        menus={storeDetail.menus.map((menu) => ({
          id: menu.id,
          name: menu.name,
          price: menu.price,
          reviewEvent: menu.reviewEvent,
          imageUrl: menu.imageUrl,
        }))}
        onMenuClick={handleMenuClick}
        selectedMenuId={selectedMenu?.id ?? null}
      />

      {/* Review Section — 그 가게에 달린 리뷰 전부 */}
      <StoreReviewSection storeId={storeDetail.id} />

      {/* Order Button Section */}
      <div
        className="fixed
                  bottom-0
                  left-1/2
                  w-full
                  max-w-[860px]
                  -translate-x-1/2"
      >
        <div className="w-full">
          <div className="bg-gradient-to-t from-white via-white/90 to-transparent p-5">
            {orderError && (
              <p className="mb-2 text-center text-sm text-red-600">
                {orderError}
              </p>
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
    </div>
  );
}
