import { useEffect, useState } from "react";
import { Button } from "@/shared/ui";
import { useIsMobile } from "@/shared/hooks";
import type { ReviewStatus } from "@/entities/order";
import {
  getRemainingReviewTime,
  formatTimeRemaining,
} from "@/entities/order/reviewTime";

export interface ReviewButtonProps {
  reviewDeadline: string;
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
    if (reviewStatus !== 'available') return;

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

  if (reviewStatus === 'pending') {
    return (
      <Button variant="secondary" size="large" fullWidth disabled>
        검토중
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
