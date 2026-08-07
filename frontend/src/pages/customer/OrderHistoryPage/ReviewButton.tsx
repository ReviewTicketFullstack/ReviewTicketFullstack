import { useEffect, useState } from "react";
import { Button } from "@/shared/ui";
import { useIsMobile } from "@/shared/hooks";
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
  const isMobile = useIsMobile();
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

  const handleReviewClick = () => {
    if (!isMobile) {
      alert("리뷰 작성은 모바일에서만 가능합니다.");
      return;
    }
    onReviewClick();
  };

  return (
    <Button
      variant="primary"
      size="large"
      fullWidth
      disabled={isDisabled}
      onClick={handleReviewClick}
    >
      {buttonText}
    </Button>
  );
}
