import { useState } from 'react';
import { Button, ImageUploadOverlay } from '@/shared/ui';
import { useAuth } from '@/app/providers';
import { MenuSampleImages, SAMPLE_IMAGE_COUNT } from './MenuSampleImages';
import type { MenuItem } from '@/api/storeApi';

// FE-2.3: 메뉴 프로필 설정 / 리뷰 설정
interface MenuEditModalProps {
  menu: MenuItem;
  onClose: () => void;
  onApply: (patch: { reviewEvent: boolean; imageUrls: (string | null)[] }) => void;
}
// FE-2.6: 가격은 이 모달에서 못 고친다 — 버튼처럼 보여도 클릭 안 되는 게 맞음
const lockedFieldClassName =
  'w-full cursor-not-allowed rounded-lg border border-neutral-200 bg-neutral-100 px-3 py-2 text-sm text-neutral-400';

export function MenuEditModal({ menu, onClose, onApply }: MenuEditModalProps) {
  const { user } = useAuth();
  const [hasReviewEvent, setHasReviewEvent] = useState(menu.reviewEvent);
  // 표본 사진 5칸. 서버가 아직 한 장(menu_image_url)만 들고 있어 0번에 넣고 시작한다.
  const [imageUrls, setImageUrls] = useState<(string | null)[]>(() =>
    Array.from({ length: SAMPLE_IMAGE_COUNT }, (_, i) =>
      i === 0 ? menu.imageUrl : null,
    ),
  );
  const [error, setError] = useState<string | null>(null);

  const handleImageChange = (index: number, url: string) => {
    setError(null);
    setImageUrls((urls) => urls.map((u, i) => (i === index ? url : u)));
  };

  return (
    // 모달 오버레이 — 클릭 시 닫힘 
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-6"
      onClick={onClose}
    >
      {/*모달 본문 */}
      <div
        className="flex w-full max-w-2xl flex-col gap-8 rounded-2xl bg-white p-10 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <MenuSampleImages
          imageUrls={imageUrls}
          onChange={handleImageChange}
          onError={setError}
        />
        {/* 로그인한 사용자 이름(가게명) 표시 */}
        <div className="w-fit rounded-lg bg-neutral-200 px-4 py-2 text-sm font-semibold">
          {user?.displayName}
        </div>
        {/* 메뉴 썸네일 + 이름/가격 + 적용 버튼 */}
        <div className="flex items-center gap-4">
          {/* 위 표본 1번 칸과 같은 사진을 비춘다 — 한쪽에서 올리면 둘 다 바뀐다 */}
          <ImageUploadOverlay
            src={imageUrls[0]}
            alt={menu.name}
            className="h-16 w-16 flex-shrink-0 rounded-lg bg-gray-200"
            onUploaded={(uploaded) => handleImageChange(0, uploaded.url)}
            onError={setError}
          />
          <div className="flex-1">
            <div className="text-lg font-bold">{menu.name}</div>
            <div className="text-sm text-gray-600">{menu.price.toLocaleString('ko-KR')}원</div>
          </div>
          <Button
            size="small"
            onClick={() => onApply({ reviewEvent: hasReviewEvent, imageUrls })}
          >
            적용
          </Button>
        </div>
        {/* 메뉴 설정 — 이미지/가격은 읽기 전용*/}         
        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-neutral-600">메뉴 설정</span>
          
          <div className="flex flex-col gap-1">
            <span className="text-xs text-neutral-400">
              메뉴 이미지 — 위 사진에 마우스를 올려 바꿔요. 손님이 올린 리뷰
              사진을 이 사진들과 대조해 판정해요.
            </span>
            {error && <span className="text-xs text-brand-900">{error}</span>}
          </div>

          <div className="flex items-center gap-2">
            <span className="w-25 flex-shrink-0 text-xs text-neutral-400">가격</span>
            <div className={`flex-1 ${lockedFieldClassName}`}>{menu.price.toLocaleString('ko-KR')}</div>
            <span className="flex-shrink-0 text-xs text-neutral-400">원</span>
          </div>
           </div>

   {/* 리뷰 이벤트 설정 — 이미지+후기 / 설정안함 선택 */}
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

        {/* 메뉴 저장 API 가 붙으면 이 줄을 지운다 */}
        <p className="text-xs text-neutral-400">
          저장 기능을 연결하기 전이라, 새로고침하면 되돌아가요.
        </p>
      </div>
    </div>
  );
}
