import { useEffect, useRef, useState } from "react";
import { Card } from "@/shared/ui";
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

function getCachedPage(): number {
  const cached = localStorage.getItem(STORE_CACHE_KEY);
  if (!cached) return 1;

  try {
    const { page } = JSON.parse(cached);
    return page || 1;
  } catch {
    return 1;
  }
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

const FOODS = ["치킨윙", "피자", "비빔밥", "라멘", "햄버거"];

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
    if (cached) setStores(cached);

    // 캐시가 있어도 서버에 반드시 다시 물어본다. 이 목록에는 리뷰 수와 평균
    // 별점이 함께 들어 있는데, 그 둘은 리뷰가 등록될 때마다 바뀌는 값이다.
    // 캐시에서 멈추면 리뷰를 써도 홈 화면은 자정까지 예전 숫자(리뷰 0)를
    // 보여줘, 등록이 안 된 것처럼 보인다.
    getStores(0, 20)
      .then((data) => {
        setStores(data);
        if (data.length > 0) setCachedStores(data);
      })
      .catch(() => {
        // 서버에 닿지 못하면 위에서 그린 캐시를 그대로 둔다. 캐시도 없으면 빈 상태다.
      });
  }, []);

  useEffect(() => {
    // 첫 페이지 데이터 로드
    if (stores.length === 0 && page === 0 && !isLoading) {
      setIsLoading(true);
      getStores(0, 20)
        .then((data) => {
          if (data.length === 0) {
            setHasMore(false);
          } else {
            setStores(data);
            setCachedStores(data, 1);
            setPage(1);
          }
        })
        .catch(() => {
          // 서버에 닿지 못하면 무시한다.
        })
        .finally(() => {
          setIsLoading(false);
        });
    }
  }, []);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoading && page > 0) {
          setIsLoading(true);
          getStores(page, 20)
            .then((data) => {
              if (data.length === 0) {
                setHasMore(false);
              } else {
                setStores((prev) => {
                  const updated = [...prev, ...data];
                  setCachedStores(updated, page + 1);
                  return updated;
                });
                setPage((prev) => prev + 1);
              }
            })
            .catch(() => {
              // 서버에 닿지 못하면 무시한다.
            })
            .finally(() => {
              setIsLoading(false);
            });
        }
      },
      { threshold: 0.5 },
    );

    if (sentinelRef.current) {
      observer.observe(sentinelRef.current);
    }

    return () => observer.disconnect();
  }, [page, isLoading, hasMore]);

  return (
    <div className="space-y-4 px-5 py-6">
      {/* Greeting Card Section */}
      <div className="max-w-[480px] w-full">
        <Card className="w-full mr-auto flex gap-4 p-4">
          <div className="space-y-2">
            <p className="text-base font-bold text-ink-900">
              안녕하세요 {user?.displayName}님
            </p>
            <div className="flex items-baseline gap-1 text-base font-bold text-ink-900">
              <span>오늘 식사는</span>
              <div className="inline-flex h-6 overflow-hidden">
                <div
                  className={
                    selectedFoodIndex !== null ? "animate-food-slot" : ""
                  }
                  style={
                    selectedFoodIndex !== null
                      ? (() => {
                          const finalPos = -(
                            19 * 5 * 24 +
                            selectedFoodIndex * 24
                          );
                          return {
                            "--final-translate-y": `${finalPos}px`,
                            "--pos-60": `${finalPos * 0.92}px`,
                            "--pos-75": `${finalPos * 0.96}px`,
                            "--pos-87": `${finalPos * 0.98}px`,
                            "--pos-95": `${finalPos * 0.995}px`,
                          } as React.CSSProperties;
                        })()
                      : {}
                  }
                >
                  {Array.from({ length: 20 })
                    .flatMap(() => FOODS)
                    .map((food, index) => (
                      <div key={index} className="h-6 flex items-center">
                        {food}
                      </div>
                    ))}
                </div>
              </div>
              <span>어떠세요?</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Store List Section */}
      <div className="space-y-4">
        {stores.map((store) => (
          <StoreCard
            key={store.id}
            storeId={store.id}
            storeName={store.name}
            rating={store.rating}
            reviewCount={store.reviewCount.toString()}
            imageUrl={store.imageUrl}
            hasReviewEvent={store.hasReviewEvent}
          />
        ))}
      </div>

      {/* Infinite Scroll Sentinel */}
      <div ref={sentinelRef} className="h-4" />

      {/* Loading Indicator */}
      {isLoading && (
        <div className="py-4 text-center text-sm text-ink-500">로딩 중...</div>
      )}
    </div>
  );
}
