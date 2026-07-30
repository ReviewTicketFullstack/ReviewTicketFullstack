import { useEffect, useState } from 'react';
import { Button } from '@/shared/ui';
import { useIsMobile } from '@/shared/hooks';
import {
  getRemainingReviewTime,
  formatTimeRemaining,
} from '@/features/order/orderStorage';

export interface ReviewButtonProps {
  reviewDeadline: string;
  hasReviewBadge: boolean;
  onReviewClick: () => void;
}

export function ReviewButton({
  reviewDeadline,
  hasReviewBadge,
  onReviewClick,
}: ReviewButtonProps) {
  const isMobile = useIsMobile();
  const [remainingTime, setRemainingTime] = useState<number>(0);
  const [isExpired, setIsExpired] = useState<boolean>(false);

  useEffect(() => {
    // Initial calculation
    const time = getRemainingReviewTime(reviewDeadline);
    setRemainingTime(time);
    setIsExpired(time <= 0);

    // Timer interval
    const interval = setInterval(() => {
      const currentTime = getRemainingReviewTime(reviewDeadline);
      setRemainingTime(currentTime);
      setIsExpired(currentTime <= 0);
    }, 100);

    return () => clearInterval(interval);
  }, [reviewDeadline]);

  if (!hasReviewBadge) {
    return (
      <Button
        variant="secondary"
        size="large"
        fullWidth
        disabled
      >
        리뷰작성 대상이 아닙니다
      </Button>
    );
  }

  const isDisabled = isExpired;
  const buttonText = isExpired
    ? '리뷰작성 가능시간 초과'
    : `리뷰작성 ${formatTimeRemaining(remainingTime)}`;

  const handleReviewClick = () => {
    if (!isMobile) {
      alert('리뷰 작성은 모바일에서만 가능합니다.');
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
