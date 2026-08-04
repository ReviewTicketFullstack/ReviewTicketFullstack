import { useState } from 'react';
import { Button } from '@/shared/ui';
import { store } from '../StoreManagementPage/mockData';
import { SamplePhotoStrip } from './SamplePhotoStrip';
import type { MenuListItem as MenuListItemType } from './mockData';

interface MenuEditModalProps {
  menu: MenuListItemType;
  onClose: () => void;
  onApply: (hasReviewEvent: boolean) => void;
}

const lockedFieldClassName =
  'w-full cursor-not-allowed rounded-lg border border-neutral-200 bg-neutral-100 px-3 py-2 text-sm text-neutral-400';

export function MenuEditModal({ menu, onClose, onApply }: MenuEditModalProps) {
  const [hasReviewEvent, setHasReviewEvent] = useState(menu.hasReviewEvent);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-6"
      onClick={onClose}
    >
      <div
        className="flex w-full max-w-md flex-col gap-6 rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <SamplePhotoStrip />

        <div className="w-fit rounded-lg bg-neutral-200 px-4 py-2 text-sm font-semibold">
          {store.name}
        </div>

        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 flex-shrink-0 items-center justify-center rounded-lg bg-gray-200">
            <span className="text-xs text-gray-400">Image</span>
          </div>
          <div className="flex-1">
            <div className="text-lg font-bold">{menu.name}</div>
            <div className="text-sm text-gray-600">{menu.price.toLocaleString('ko-KR')}원</div>
          </div>
          <Button size="small" onClick={() => onApply(hasReviewEvent)}>
            적용
          </Button>
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-neutral-600">메뉴 설정</span>
          <div className={lockedFieldClassName}>메뉴 이미지</div>
          <div className={lockedFieldClassName}>{menu.price.toLocaleString('ko-KR')}원</div>
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-neutral-600">리뷰 이벤트 설정</span>
          <button
            type="button"
            onClick={() => setHasReviewEvent(true)}
            className={`rounded-lg border px-4 py-2 text-left text-sm ${
              hasReviewEvent
                ? 'border-brand-800 bg-brand-50 font-semibold text-brand-800'
                : 'border-neutral-200 text-neutral-700'
            }`}
          >
            이미지 + 후기
          </button>
          <button
            type="button"
            onClick={() => setHasReviewEvent(false)}
            className={`rounded-lg border px-4 py-2 text-left text-sm ${
              !hasReviewEvent
                ? 'border-brand-800 bg-brand-50 font-semibold text-brand-800'
                : 'border-neutral-200 text-neutral-700'
            }`}
          >
            설정안함
          </button>
        </div>
      </div>
    </div>
  );
}
