import { useEffect, useState } from "react";
import { Button, EmptyState, Loading } from "@/shared/ui";
import { DetailStoreCard } from "./DetailStoreCard";
import { MenuListCard, type MenuItemData } from "./MenuListCard";
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
      <div className="flex min-h-screen items-center justify-center">
        <Loading />
      </div>
    );
  }

  if (!storeDetail || loadError) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <EmptyState
          icon="🔍"
          message={loadError || "가게를 찾을 수 없어요."}
          action={
            <Button variant="secondary" size="medium" onClick={() => navigate("/")}>
              홈으로 가기
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5 px-5 pt-3 pb-32">
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
        onReviewClick={() => navigate(`/order/${storeDetail.id}/reviews`)}
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

      {/* Order Button Section — 하단 고정 CTA 독.
          그라디언트 대신 Surface 단색 + shadow-dock 으로 경계를 만든다(design.md). */}
      <div className="fixed bottom-0 left-1/2 w-full max-w-[860px] -translate-x-1/2 bg-surface shadow-dock">
        <div className="flex flex-col gap-2 px-5 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]">
          {orderError && (
            <p role="alert" className="text-center text-xs text-brand-900">
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
  );
}
