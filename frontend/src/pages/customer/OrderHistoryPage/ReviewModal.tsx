import { useState } from 'react';
import { Button } from '@/shared/ui';
import { Modal } from '@/shared/ui/Modal/Modal';
import { useCameraCapture } from '@/shared/hooks';

export interface ReviewModalProps {
  open: boolean;
  onClose: () => void;
  storeName: string;
  menuName: string;
  onSubmitSuccess?: () => void;
}

export interface ReviewData {
  rating: number;
  reviewText: string;
  photo: string | null; // Base64 encoded image
}

export function ReviewModal({
  open,
  onClose,
  storeName,
  menuName,
  onSubmitSuccess,
}: ReviewModalProps) {
  const [rating, setRating] = useState<number>(0);
  const [reviewText, setReviewText] = useState<string>('');

  const { photo, removePhoto, capturePhoto, handleFileSelected, fileInputRef, error: cameraError } = useCameraCapture();

  const handleSubmit = () => {
    if (rating === 0) {
      alert('평점을 선택해주세요.');
      return;
    }

    if (!reviewText.trim()) {
      alert('리뷰 내용을 작성해주세요.');
      return;
    }

    const reviewData: ReviewData = {
      rating,
      reviewText,
      photo: photo?.photoData ?? null,
    };

    console.log('리뷰 제출:', reviewData);
    alert('리뷰가 작성되었습니다!');

    // 상태 초기화
    setRating(0);
    setReviewText('');
    removePhoto();

    // 제출 성공 콜백
    onSubmitSuccess?.();

    onClose();
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

          {/* 버튼 */}
          <div className="flex gap-3">
            <Button
              variant="secondary"
              size="large"
              fullWidth
              onClick={onClose}
            >
              취소
            </Button>
            <Button
              variant="primary"
              size="large"
              fullWidth
              onClick={handleSubmit}
            >
              제출
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
