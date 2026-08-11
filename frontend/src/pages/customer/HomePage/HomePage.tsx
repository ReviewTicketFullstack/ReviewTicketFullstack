import { useEffect, useState } from "react";
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

function setCachedStores(stores: Store[]) {
  const nextMidnight = new Date();
  nextMidnight.setHours(24, 0, 0, 0);

  localStorage.setItem(
    STORE_CACHE_KEY,
    JSON.stringify({
      stores,
      timestamp: Date.now(),
      expiresAt: nextMidnight.getTime(),
    }),
  );
}

export function HomePage() {
  const { user } = useAuth();
  const [stores, setStores] = useState<Store[]>([]);

  useEffect(() => {
    // 캐시가 있으면 먼저 그려 첫 화면이 비어 보이지 않게 한다.
    const cached = getCachedStores();
    if (cached) setStores(cached);

    // 캐시가 있어도 서버에 반드시 다시 물어본다. 이 목록에는 리뷰 수와 평균
    // 별점이 함께 들어 있는데, 그 둘은 리뷰가 등록될 때마다 바뀌는 값이다.
    // 캐시에서 멈추면 리뷰를 써도 홈 화면은 자정까지 예전 숫자(리뷰 0)를
    // 보여줘, 등록이 안 된 것처럼 보인다.
    getStores()
      .then((data) => {
        setStores(data);
        if (data.length > 0) setCachedStores(data);
      })
      .catch(() => {
        // 서버에 닿지 못하면 위에서 그린 캐시를 그대로 둔다. 캐시도 없으면 빈 상태다.
      });
  }, []);

  return (
    <div className="space-y-6 px-5 py-6">
      {/* Greeting Card Section */}
      <div className="grid grid-cols-2 gap-4">
        <Card className="flex items-center justify-center p-6">
          <div className="text-center">
            <p className="text-3xl font-bold leading-14 text-left">
              안녕하세요!
            </p>
            <p className="text-6xl font-bold leading-14 text-left">
              {user?.displayName} 님!
            </p>
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
          />
        ))}
      </div>
    </div>
  );
}
