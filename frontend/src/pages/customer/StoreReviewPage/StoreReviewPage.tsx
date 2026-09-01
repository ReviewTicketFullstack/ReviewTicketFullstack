import { useNavigate, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { Button, EmptyState } from "@/shared/ui";
import { StoreReviewSection } from "../OrderPage/StoreReviewSection";

/**
 * 가게 리뷰 전용 화면.
 *
 * 진입점은 가게 상세(주문 화면)의 리뷰 개수뿐이다. 하단 네비게이션에는
 * 리뷰 탭을 두지 않는다.
 */
export function StoreReviewPage() {
  const { storeId } = useParams();
  const navigate = useNavigate();

  // 주소를 직접 고쳐 들어온 경우다. 숫자가 아니면 조회할 수 없다.
  if (!storeId || Number.isNaN(Number(storeId))) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <EmptyState
          icon="🔍"
          message="가게를 찾을 수 없어요."
          action={
            <Button variant="secondary" size="medium" onClick={() => navigate("/home")}>
              홈으로 가기
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 py-5">
      <div className="px-5">
        <button
          type="button"
          onClick={() => navigate(-1)}
          aria-label="가게 화면으로 돌아가기"
          className="-ml-3 flex size-11 items-center justify-center rounded-lg text-ink-900 hover:bg-fill-100 active:bg-line-100"
        >
          <ChevronLeft size={24} />
        </button>
      </div>

      <StoreReviewSection storeId={Number(storeId)} />
    </div>
  );
}
