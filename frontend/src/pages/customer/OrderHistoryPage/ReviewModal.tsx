import { useState } from 'react';
import { Button } from '@/shared/ui';
import { Modal } from '@/shared/ui/Modal/Modal';
import { useCameraCapture } from '@/shared/hooks';
import { createReview } from '@/api/reviewApi';
import { ApiError } from '@/shared/api';
import type { ID } from '@/entities/order';

export interface ReviewModalProps {
  open: boolean;
  onClose: () => void;
  orderId: ID;
  storeName: string;
  menuName: string;
  onSubmitSuccess?: () => void;
}

/** 후기 길이 제한. 서버도 같은 값으로 검사한다. */
const CONTENT_MIN_LENGTH = 10;
const CONTENT_MAX_LENGTH = 50;

/**
 * 제출 실패 사유를 화면 문구로 옮긴다.
 *
 * 서버는 errorCode 만 보내고 문구는 화면이 채운다. 사진이 메뉴와 다른 경우
 * (IMAGE_NOT_MATCHED)는 오류가 아니라 정상적으로 자주 일어나는 결과라,
 * 유사도를 함께 보여주고 다시 찍도록 안내한다.
 */
function toMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return '리뷰를 등록하지 못했습니다.';

  switch (error.errorCode) {
    case 'IMAGE_NOT_MATCHED': {
      const similarity = error.detail?.imageSimilarity;
      const percent =
        similarity === undefined ? null : Math.round(similarity * 100);
      return percent === null
        ? '주문한 메뉴와 다른 사진으로 보입니다. 음식이 잘 보이게 다시 찍어 주세요.'
        : `주문한 메뉴와 일치율이 ${percent}% 입니다. 음식이 잘 보이게 다시 찍어 주세요.`;
    }
    case 'REVIEW_PERIOD_EXPIRED':
      return '리뷰 작성 시간이 지났습니다.';
    case 'REVIEW_ALREADY_EXISTS':
      return '이미 리뷰를 작성한 주문입니다.';
    case 'REVIEW_EVENT_NOT_APPLIED':
      return '리뷰이벤트에 참여하지 않은 주문입니다.';
    case 'IMAGE_TOO_SMALL':
      return '사진 화질이 너무 낮습니다. 카메라로 다시 찍어 주세요.';
    case 'FILE_TOO_LARGE':
      return '사진 용량이 너무 큽니다.';
    case 'UNSUPPORTED_IMAGE_TYPE':
      return '지원하지 않는 사진 형식입니다.';
    case 'AI_SERVER_UNAVAILABLE':
      return '사진 확인이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.';
    default:
      return error.message;
  }
}

export function ReviewModal({
  open,
  onClose,
  orderId,
  storeName,
  menuName,
  onSubmitSuccess,
}: ReviewModalProps) {
  const [rating, setRating] = useState<number>(0);
  const [reviewText, setReviewText] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { photo, photoFile, removePhoto, capturePhoto, handleFileSelected, fileInputRef, error: cameraError } = useCameraCapture();

  const handleSubmit = async () => {
    if (rating === 0) {
      setSubmitError('평점을 선택해주세요.');
      return;
    }

    const content = reviewText.trim();
    if (content.length < CONTENT_MIN_LENGTH || content.length > CONTENT_MAX_LENGTH) {
      setSubmitError(
        `리뷰는 ${CONTENT_MIN_LENGTH}자 이상 ${CONTENT_MAX_LENGTH}자 이하로 작성해 주세요.`,
      );
      return;
    }

    if (!photoFile) {
      setSubmitError('사진을 촬영해 주세요.');
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await createReview(orderId, rating, content, photoFile);

      // 통과했을 때만 입력을 비운다. 실패하면 별점과 후기를 그대로 두어
      // 사진만 다시 찍어 재시도할 수 있게 한다.
      setRating(0);
      setReviewText('');
      removePhoto();

      onSubmitSuccess?.();
      onClose();
    } catch (error) {
      setSubmitError(toMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };


  return (
    <>
      <Modal open={open} onClose={onClose}>
        <div className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold">리뷰 작성</h2>
            <p className="text-gray-600 mt-2">
              {storeName} - {menuName}
            </p>
          </div>

          <div className="space-y-4">
            {/* 평점 */}
            <div>
              <label className="block text-sm font-semibold mb-2">평점</label>
              <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    onClick={() => setRating(star)}
                    className={`text-3xl transition-transform ${
                      star <= rating ? 'scale-110' : 'opacity-40 hover:opacity-70'
                    }`}
                  >
                    ⭐
                  </button>
                ))}
              </div>
            </div>

            {/* 리뷰 내용 */}
            <div>
              <label className="block text-sm font-semibold mb-2">리뷰 내용</label>
              <textarea
                value={reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                className="w-full p-3 border border-gray-200 rounded-lg resize-none focus:outline-none focus:border-red-700"
                rows={4}
                placeholder="리뷰를 작성해주세요."
              />
            </div>

            {/* 카메라 캡처 */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-semibold">사진</label>
                {photo && (
                  <button
                    onClick={removePhoto}
                    className="text-xs text-gray-500 hover:text-gray-700"
                  >
                    제거
                  </button>
                )}
              </div>

              {photo ? (
                <div className="flex gap-3 items-center">
                  <img
                    src={photo.photoData}
                    alt="촬영한 사진"
                    className="size-20 rounded-lg object-cover border border-gray-200"
                  />
                  <span className="text-sm text-gray-600">1장 첨부됨</span>
                </div>
              ) : (
                <button
                  onClick={capturePhoto}
                  className="w-full px-4 py-2 border-2 border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-gray-400 transition-colors"
                >
                  📷 사진 촬영
                </button>
              )}

              {cameraError && (
                <p className="text-sm text-red-600 mt-2">{cameraError}</p>
              )}
            </div>
          </div>

          {/* 제출 실패 사유 — 별점과 후기는 그대로 두고 이 문구만 갱신한다 */}
          {submitError && (
            <p className="text-sm text-red-600">{submitError}</p>
          )}

          {/* 버튼 */}
          <div className="flex gap-3">
            <Button
              variant="secondary"
              size="large"
              fullWidth
              onClick={onClose}
              disabled={isSubmitting}
            >
              취소
            </Button>
            <Button
              variant="primary"
              size="large"
              fullWidth
              onClick={handleSubmit}
              disabled={isSubmitting}
            >
              {isSubmitting ? '확인 중...' : '제출'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 숨겨진 카메라 입력 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={handleFileSelected}
      />
    </>
  );
}
