import { Button } from '@/shared/ui';
import { Modal } from '@/shared/ui/Modal/Modal';

export interface ReviewModalProps {
  open: boolean;
  onClose: () => void;
  storeName: string;
  menuName: string;
}

export function ReviewModal({
  open,
  onClose,
  storeName,
  menuName,
}: ReviewModalProps) {
  const handleSubmit = () => {
    alert('리뷰가 작성되었습니다!');
    onClose();
  };

  return (
    <Modal open={open} onClose={onClose}>
      <div className="space-y-6">
        <div>
          <h2 className="text-2xl font-bold">리뷰 작성</h2>
          <p className="text-gray-600 mt-2">
            {storeName} - {menuName}
          </p>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-semibold mb-2">평점</label>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  className="text-2xl hover:scale-110 transition-transform"
                >
                  ⭐
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold mb-2">리뷰 내용</label>
            <textarea
              className="w-full p-3 border border-gray-200 rounded-lg resize-none focus:outline-none focus:border-red-700"
              rows={4}
              placeholder="리뷰를 작성해주세요."
            />
          </div>
        </div>

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
  );
}
