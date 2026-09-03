import { useEffect, useState } from "react";
import { Button } from "@/shared/ui";
import type { ReviewStatus } from "@/entities/order";
import {
  getRemainingReviewTime,
  formatTimeRemaining,
} from "@/entities/order/reviewTime";

export interface ReviewButtonProps {
  /** 마감 시각. 리뷰이벤트에 참여하지 않은 주문은 null 이다 */
  reviewDeadline: string | null;
  hasReviewBadge: boolean;
  reviewStatus: ReviewStatus;
  onReviewClick: () => void;
}

export function ReviewButton({
  reviewDeadline,
  hasReviewBadge,
  reviewStatus,
  onReviewClick,
}: ReviewButtonProps) {
  const [remainingTime, setRemainingTime] = useState<number>(0);
  const [isExpired, setIsExpired] = useState<boolean>(false);

  useEffect(() => {
    if (reviewStatus !== 'available' || !reviewDeadline) return;

    const time = getRemainingReviewTime(reviewDeadline);
    setRemainingTime(time);
    setIsExpired(time <= 0);

    const interval = setInterval(() => {
      const currentTime = getRemainingReviewTime(reviewDeadline);
      setRemainingTime(currentTime);
      setIsExpired(currentTime <= 0);
    }, 300);

    return () => clearInterval(interval);
  }, [reviewDeadline, reviewStatus]);

  if (!hasReviewBadge || reviewStatus === 'not_available') {
    return (
      <Button variant="secondary" size="large" fullWidth disabled>
        리뷰작성 대상이 아닙니다
      </Button>
    );
  }

  if (reviewStatus === 'done') {
    return (
      <Button variant="secondary" size="large" fullWidth disabled>
        등록완료
      </Button>
    );
  }

  // 서버가 조회 시점에 이미 마감을 판정해 준다. 그 뒤 화면에 머무는 동안
  // 0 초가 되는 경우는 아래 카운트다운(isExpired)이 잡는다.
  if (reviewStatus === 'expired') {
    return (
      <Button variant="secondary" size="large" fullWidth disabled>
        리뷰작성 가능시간 초과
      </Button>
    );
  }

  const isDisabled = isExpired;
  const buttonText = isExpired
    ? "리뷰작성 가능시간 초과"
    : `리뷰작성 ${formatTimeRemaining(remainingTime)}`;

  // 모바일에서만 열리도록 막아 두었었다. 카메라로 찍는 것이 원래 동선이지만,
  // PC 에서 아예 못 열면 개발·검수 때 흐름을 확인할 방법이 없어 그 차단을
  // 걷어냈다. PC 는 카메라 대신 파일 선택 창이 열린다(ReviewModal 의 input).
  return (
    <Button
      variant="primary"
      size="large"
      fullWidth
      disabled={isDisabled}
      onClick={onReviewClick}
    >
      {buttonText}
    </Button>
  );
}
