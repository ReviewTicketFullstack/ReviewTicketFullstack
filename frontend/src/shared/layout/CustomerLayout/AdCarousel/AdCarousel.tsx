import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { AD_SLIDES } from "./slides";

/** 자동 전환 간격. 사용자가 조작 중일 때는 멈춘다. */
const AUTO_ADVANCE_MS = 2000;

/**
 * 상단 광고 캐러셀.
 *
 * 레이아웃 프레임 폭을 꽉 채우는 전면 배너다. 카드가 아니라 배경 자체가
 * 슬라이드라, Card 대신 채도 높은 단색 배경 + 흰 전경으로 둔다.
 * 새 의존성은 쓰지 않는다 — 전환은 translateX 한 줄이면 충분하다.
 */
export function AdCarousel() {
  const [index, setIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const total = AD_SLIDES.length;

  useEffect(() => {
    if (isPaused) return;

    const timer = window.setInterval(() => {
      setIndex((prev) => (prev + 1) % total);
    }, AUTO_ADVANCE_MS);

    return () => window.clearInterval(timer);
  }, [isPaused, total]);

  const goTo = (next: number) => setIndex(((next % total) + total) % total);

  const controlClass =
    "absolute top-1/2 z-10 flex size-11 -translate-y-1/2 items-center justify-center rounded-full text-white transition-colors hover:bg-white/20 active:bg-white/30";

  return (
    <section
      aria-roledescription="carousel"
      aria-label="광고"
      className="relative w-full overflow-hidden"
      // 마우스를 올리거나 키보드 포커스가 들어오면 자동 전환을 멈춘다.
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onFocus={() => setIsPaused(true)}
      onBlur={() => setIsPaused(false)}
    >
      <div
        className="flex transition-transform duration-500 ease-out motion-reduce:transition-none"
        style={{ transform: `translateX(-${index * 100}%)` }}
      >
        {AD_SLIDES.map((slide, slideIndex) => (
          <div
            key={slide.id}
            role="group"
            aria-roledescription="slide"
            aria-label={`${slideIndex + 1} / ${total}`}
            aria-hidden={slideIndex !== index}
            className={`flex w-full shrink-0 items-center gap-5 px-5 py-8 ${slide.backgroundClass} ${slide.foregroundClass}`}
          >
            <div className="flex min-w-0 flex-1 flex-col gap-2">
              <span className="text-xs font-semibold opacity-80">
                {slide.eyebrow}
              </span>

              {/* 광고 제목은 화면에서 가장 크게. design.md 타입 상한 32px */}
              <p className="text-3xl font-bold leading-tight">{slide.title}</p>

              <p className="text-sm leading-relaxed opacity-90">
                {slide.description}
              </p>
            </div>

            <div className="size-20 shrink-0">{slide.media}</div>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => goTo(index - 1)}
        aria-label="이전 광고"
        className={`${controlClass} left-1`}
      >
        <ChevronLeft size={20} aria-hidden="true" />
      </button>

      <button
        type="button"
        onClick={() => goTo(index + 1)}
        aria-label="다음 광고"
        className={`${controlClass} right-1`}
      >
        <ChevronRight size={20} aria-hidden="true" />
      </button>
    </section>
  );
}
