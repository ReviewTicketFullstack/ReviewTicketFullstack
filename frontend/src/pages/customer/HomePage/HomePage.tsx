import { useEffect, useState } from "react";
import { Card } from "@/shared/ui";
import { StoreCard } from "./StoreCard";
import { useAuth } from "@/app/providers";
import { getStores } from "@/api/storeApi";
import type { Store } from "@/entities/store";

const CACHE_KEY = "stores_cache";

function getCachedStores(): Store[] | null {
  const cached = localStorage.getItem(CACHE_KEY);
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
    localStorage.removeItem(CACHE_KEY);
  }

  return null;
}

function setCachedStores(stores: Store[]) {
  const nextMidnight = new Date();
  nextMidnight.setHours(24, 0, 0, 0);

  localStorage.setItem(
    CACHE_KEY,
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
    const cached = getCachedStores();

    if (cached) {
      setStores(cached);
      return;
    }

    getStores()
      .then((data) => {
        setStores(data);
        if (data.length > 0) setCachedStores(data);
      })
      .catch(() => {
        // 서버 요청 실패, 캐시도 없으므로 빈 상태 유지
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
