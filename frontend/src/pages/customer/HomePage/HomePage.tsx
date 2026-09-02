import { useEffect, useRef, useState } from "react";
import { EmptyState, FeatureCard, Loading } from "@/shared/ui";
import { GreetingCard } from "./GreetingCard";
import { FOODS } from "./foods";
import { StoreCard } from "./StoreCard";
import { useAuth } from "@/app/providers";
import { getStores } from "@/api/storeApi";
import { STORE_CACHE_KEY } from "@/entities/store/storeCache";
import type { Store } from "@/entities/store";

function getCachedStores(): Store[] | null {
  const cached = localStorage.getItem(STORE_CACHE_KEY);
  if (!cached) return null;

  try {
    const { stores, timestamp } = JSON.parse(cached);
    // 빈 배열은 캐시하지 않는다 — "아직 가게가 없다"는 결과를 자정까지 그대로
    // 믿으면, 그사이 새로 생긴 가게가 당일 내내 홈 화면에 안 뜬다.
    if (stores.length === 0) return null;

    const nextMidnight = new Date();
    nextMidnight.setHours(24, 0, 0, 0);

    if (timestamp < nextMidnight.getTime()) {
      return stores;
    }
  } catch {
    localStorage.removeItem(STORE_CACHE_KEY);
  }

  return null;
}

function setCachedStores(stores: Store[], page: number = 1) {
  const nextMidnight = new Date();
  nextMidnight.setHours(24, 0, 0, 0);

  localStorage.setItem(
    STORE_CACHE_KEY,
    JSON.stringify({
      stores,
      page,
      timestamp: Date.now(),
      expiresAt: nextMidnight.getTime(),
    }),
  );
}

export function HomePage() {
  const { user } = useAuth();
  const [stores, setStores] = useState<Store[]>([]);
  const [selectedFoodIndex, setSelectedFoodIndex] = useState<number | null>(
    null,
  );
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setSelectedFoodIndex(Math.floor(Math.random() * FOODS.length));
  }, []);

  useEffect(() => {
    // 캐시가 있으면 먼저 그려 첫 화면이 비어 보이지 않게 한다.
    const cached = getCachedStores();
    if (cached) {
      setStores(cached);
    }

    // 캐시가 있어도 서버에 반드시 다시 물어본다. 이 목록에는 리뷰 수와 평균
    // 별점이 함께 들어 있는데, 그 둘은 리뷰가 등록될 때마다 바뀌는 값이다.
    // 캐시에서 멈추면 리뷰를 써도 홈 화면은 자정까지 예전 숫자(리뷰 0)를
    // 보여줘, 등록이 안 된 것처럼 보인다.
    // 첫 페이지는 0번이다. 여기서 받은 결과로 page 를 1로 올려야 아래 옵저버의
    // page <= 0 가드가 풀린다 — 올리지 않으면 무한 스크롤이 한 번도 발동하지 않는다.
    getStores(0, 20)
      .then((data) => {
        setStores(data);
        setPage(1);
        setHasMore(data.length === 20);
        if (data.length > 0) {
          setCachedStores(data, 1);
        }
      })
      .catch(() => {
        // 서버에 닿지 못하면 위에서 그린 캐시를 그대로 둔다. 캐시도 없으면 빈 상태다.
      });
  }, []);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || !hasMore || isLoading || page <= 0) {
          return;
        }

        setIsLoading(true);
        getStores(page, 20)
          .then((data) => {
            setStores((prev) => {
              const updated = [...prev, ...data];
              setCachedStores(updated, page + 1);
              return updated;
            });

            if (data.length < 20) {
              setHasMore(false);
            } else {
              setPage((prev) => prev + 1);
            }
          })
          .catch(() => {
            // 서버에 닿지 못하면 무시한다.
          })
          .finally(() => {
            setIsLoading(false);
          });
      },
      { threshold: 0.5 },
    );

    if (sentinelRef.current) {
      observer.observe(sentinelRef.current);
    }

    return () => {
      observer.disconnect();
    };
  }, [page, isLoading, hasMore]);

  return (
    <div className="flex flex-col gap-8 px-5 py-6">
      {/* Greeting Section — 좁은 화면에서는 세로로 쌓이고, 폭이 나면
          인사 카드 옆 빈 자리에 안내 카드가 나란히 붙는다. */}
      <section className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <GreetingCard
          displayName={user?.displayName}
          selectedFoodIndex={selectedFoodIndex}
        />

        {/* media 를 넘기지 않으면 Lottie 플레이스홀더가 뜬다.
            실제 애니메이션은 shared/ui/FeatureCard/LottieSlot 에서 교체한다. */}
        <FeatureCard
          eyebrow="리뷰 이벤트"
          title="리뷰 쓰고 티켓 받기"
          description="리뷰 배지가 붙은 가게에서 주문하고 사진 리뷰를 남기면 티켓을 받아요."
        />
      </section>

      {/* Store List Section */}
      <section className="flex flex-col gap-3">
        <h2 className="text-base font-bold text-ink-900">
          지금 주문할 수 있는 가게
        </h2>

        {stores.length === 0 && !isLoading ? (
          <EmptyState
            icon="🍚"
            message="아직 등록된 가게가 없어요. 조금 뒤에 다시 확인해 주세요."
          />
        ) : (
          <ul className="flex flex-col gap-3">
            {stores.map((store) => (
              <li key={store.id}>
                <StoreCard
                  storeId={store.id}
                  storeName={store.name}
                  rating={store.rating}
                  reviewCount={store.reviewCount.toString()}
                  imageUrl={store.imageUrl}
                  hasReviewEvent={store.hasReviewEvent}
                />
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Infinite Scroll Sentinel */}
      <div ref={sentinelRef} className="h-4" />

      {isLoading && <Loading />}
    </div>
  );
}
