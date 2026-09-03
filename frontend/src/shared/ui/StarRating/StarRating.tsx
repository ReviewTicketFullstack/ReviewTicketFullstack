import { Star } from "lucide-react";

const MAX_RATING = 5;

export interface StarRatingProps {
  /** 0~5 사이의 정수 */
  rating: number;
  /** 별 하나의 픽셀 크기 */
  size?: number;
  className?: string;
}

/**
 * 별점 표시 전용. 받은 수만큼 채우고 나머지는 비운 별로 다섯 칸을 채운다.
 *
 * 색으로만 정보를 전달하지 않도록 aria-label 에 숫자를 함께 넣는다 —
 * 별의 채움 여부는 스크린리더에 전달되지 않는다.
 */
export function StarRating({
  rating,
  size = 14,
  className = "",
}: StarRatingProps) {
  return (
    <span
      className={`inline-flex items-center gap-0.5 ${className}`}
      role="img"
      aria-label={`별점 ${rating}점`}
    >
      {Array.from({ length: MAX_RATING }, (_, index) => (
        <Star
          key={index}
          size={size}
          aria-hidden="true"
          className={
            index < rating ? "fill-star text-star" : "fill-line-100 text-line-100"
          }
        />
      ))}
    </span>
  );
}
