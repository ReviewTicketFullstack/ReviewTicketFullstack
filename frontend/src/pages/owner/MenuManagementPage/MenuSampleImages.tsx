import { ImageUploadOverlay } from '@/shared/ui';

/** 메뉴 하나가 가질 수 있는 표본 사진 수. */
export const SAMPLE_IMAGE_COUNT = 5;

/**
 * 표본 사진 긴 변의 하한. 리뷰 사진에 요구하는 값(application.yml 의
 * reviewticket.review.min-image-long-edge)과 같게 맞춘다 — 대조하는 두 사진
 * 중 기준 쪽만 저화질이면 같은 음식을 찍은 리뷰도 유사도가 낮게 나와 거부된다.
 */
const MIN_SAMPLE_LONG_EDGE = 1920;

interface MenuSampleImagesProps {
  /** 길이 SAMPLE_IMAGE_COUNT 고정. 아직 안 올린 칸은 null */
  imageUrls: (string | null)[];
  onChange: (index: number, url: string) => void;
  onError: (message: string) => void;
}

/**
 * 메뉴 표본 사진 5칸.
 *
 * 손님이 올린 리뷰 사진을 이 사진들과 대조해 판정한다. 여러 각도를 올려 둘수록
 * 같은 음식을 다르게 찍어도 통과할 여지가 생긴다.
 *
 * 다섯 칸은 동등하다 — 대표를 고르는 자리가 아니다.
 */
export function MenuSampleImages({
  imageUrls,
  onChange,
  onError,
}: MenuSampleImagesProps) {
  return (
    <div className="flex w-full gap-2">
      {Array.from({ length: SAMPLE_IMAGE_COUNT }, (_, index) => (
        <ImageUploadOverlay
          key={index}
          src={imageUrls[index] ?? null}
          alt={`표본 사진 ${index + 1}`}
          className="aspect-square flex-1 rounded-lg bg-gray-200"
          minLongEdge={MIN_SAMPLE_LONG_EDGE}
          onUploaded={(uploaded) => onChange(index, uploaded.url)}
          onError={onError}
        />
      ))}
    </div>
  );
}
