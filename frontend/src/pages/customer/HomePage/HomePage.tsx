import { useEffect, useRef, useState } from "react";
import { EmptyState, FeatureCard, Loading } from "@/shared/ui";
import { GreetingCard } from "./GreetingCard";
import { FOODS } from "./foods";
import { StoreCard } from "./StoreCard";
import { useAuth } from "@/app/providers";
import { getStores } from "@/api/storeApi";
import { STORE_CACHE_KEY } from "@/entities/store/storeCache";
import type { Store } from "@/entities/store";
import type { StoreSort } from "@/api/storeApi";

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
  const [sort, setSort] = useState<StoreSort>("LATEST");
  const sortRef = useRef(sort);

  sortRef.current = sort;

  useEffect(() => {
    setSelectedFoodIndex(Math.floor(Math.random() * FOODS.length));
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    // 정렬 기준이 바뀌면 기존 목록을 비우고 첫 페이지부터 다시 조회
    setStores([]);
    setPage(0);
    setHasMore(true);

    // 현재 캐시는 LATEST 목록에서만 사용
    if (sort === "LATEST") {
      const cached = getCachedStores();
      if (cached) setStores(cached);
    }

    // 캐시가 있어도 서버에 반드시 다시 요청
    // 리뷰수와 평균 별점은 리뷰 등록 시 변경될 수 있기 때문이다.
    // 첫 페이지는 0번. 조회 성공 후 page 를 1로 올려 다음 페이지 요청 준비.
    getStores(0, 20, sort, controller.signal)
      .then((data) => {
        setStores(data); // 서버 결과로 교체
        setPage(1);
        setHasMore(data.length === 20);

        if (sort === "LATEST" && data.length > 0) {
          setCachedStores(data, 1);
        }
      })
      .catch((error) => {
        if (error instanceof DOMException && error.name === "AbortError")
          return;
      });

    return () => controller.abort();
  }, [sort]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || !hasMore || isLoading || page <= 0) {
          return;
        }

        setIsLoading(true);
        getStores(page, 20, sort)
          .then((data) => {
            if (sortRef.current !== sort) return;

            setStores((prev) => [...prev, ...data]);

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
  }, [page, isLoading, hasMore, sort]);

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
        <div className="flex gap-1">
          {(["LATEST", "REVIEWS"] as const).map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setSort(value)}
              aria-pressed={sort === value}
              className={`rounded-lg px-3 py-1.5 text-xs transition-colors ${
                sort === value
                  ? "font-bold text-brand-800"
                  : "font-semibold text-ink-500 hover:text-ink-900"
              }`}
            >
              {value === "LATEST" ? "최신순" : "리뷰많은순"}
            </button>
          ))}
        </div>

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
