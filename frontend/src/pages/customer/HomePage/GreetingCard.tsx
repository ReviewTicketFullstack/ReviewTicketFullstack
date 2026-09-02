import { Card } from "@/shared/ui";
import { FOODS } from "./foods";

/** 슬롯머신 한 칸의 높이(px). 아래 트랙 계산이 전부 이 값을 기준으로 돈다. */
const SLOT_HEIGHT = 24;
/** 최종 위치에 닿기 전까지 돌려 보내는 바퀴 수. */
const SPIN_ROUNDS = 19;
/** 트랙에 쌓아 두는 반복 횟수. SPIN_ROUNDS 보다 넉넉해야 빈 칸이 안 보인다. */
const TRACK_REPEATS = 20;

export interface GreetingCardProps {
  displayName?: string;
  /** 슬롯이 멈출 음식의 인덱스. 아직 안 정해졌으면 null — 애니메이션을 걸지 않는다 */
  selectedFoodIndex: number | null;
}

export function GreetingCard({
  displayName,
  selectedFoodIndex,
}: GreetingCardProps) {
  // 트랙을 끝까지 밀어 올렸을 때의 최종 오프셋. 중간 지점들은 감속 구간이다.
  const finalPos =
    selectedFoodIndex === null
      ? 0
      : -(SPIN_ROUNDS * FOODS.length * SLOT_HEIGHT +
          selectedFoodIndex * SLOT_HEIGHT);

  const slotStyle =
    selectedFoodIndex === null
      ? {}
      : ({
          "--final-translate-y": `${finalPos}px`,
          "--pos-60": `${finalPos * 0.92}px`,
          "--pos-75": `${finalPos * 0.96}px`,
          "--pos-87": `${finalPos * 0.98}px`,
          "--pos-95": `${finalPos * 0.995}px`,
        } as React.CSSProperties);

  return (
    <Card className="flex h-full flex-col justify-center gap-3 p-5">
      <p className="text-sm text-ink-700">
        안녕하세요,{" "}
        <span className="font-bold text-ink-900">{displayName}</span>님
      </p>

      <div className="flex items-baseline gap-1 text-xl font-bold text-ink-900">
        <span>오늘 식사는</span>
        <span className="inline-flex h-6 overflow-hidden text-brand-800">
          {/* 트랙. 자식이 세로로 쌓여야 하므로 block 을 명시한다 */}
          <span
            className={`block ${selectedFoodIndex !== null ? "animate-food-slot" : ""}`}
            style={slotStyle}
          >
            {Array.from({ length: TRACK_REPEATS })
              .flatMap(() => FOODS)
              .map((food, index) => (
                <span key={index} className="flex h-6 items-center">
                  {food}
                </span>
              ))}
          </span>
        </span>
        <span>어떠세요?</span>
      </div>
    </Card>
  );
}
