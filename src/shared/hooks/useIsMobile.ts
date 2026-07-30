import { useEffect, useState } from 'react';

const MOBILE_BREAKPOINT = 768; // md breakpoint

/**
 * 현재 환경이 모바일 기기인지 판단하는 hook.
 *
 * 두 조건을 모두 확인합니다:
 * 1. 뷰포트 너비가 모바일 breakpoint보다 작은지
 * 2. 기기가 터치 입력을 지원하는지 (maxTouchPoints > 0)
 *
 * 뷰포트 크기 변경 시 자동으로 업데이트되고, 이벤트 리스너를 정리합니다.
 */
export function useIsMobile(): boolean {
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;

    const isNarrow = window.innerWidth < MOBILE_BREAKPOINT;
    const hasTouch = navigator.maxTouchPoints > 0;

    return isNarrow && hasTouch;
  });

  useEffect(() => {
    const handleResize = () => {
      const isNarrow = window.innerWidth < MOBILE_BREAKPOINT;
      const hasTouch = navigator.maxTouchPoints > 0;
      setIsMobile(isNarrow && hasTouch);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return isMobile;
}
