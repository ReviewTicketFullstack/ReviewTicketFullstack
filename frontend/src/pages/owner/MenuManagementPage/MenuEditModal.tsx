import { useState } from 'react';
import { Button, ImageUploadOverlay } from '@/shared/ui';
import { useAuth } from '@/app/providers';
import { MenuSampleImages, SAMPLE_IMAGE_COUNT } from './MenuSampleImages';
import type { MenuItem } from '@/api/storeApi';

// FE-2.3: 메뉴 프로필 설정 / 리뷰 설정
interface MenuEditModalProps {
  menu: MenuItem;
  /** 서버에 이미 저장돼 있던 표본 사진 */
  initialSampleUrls?: (string | null)[];
  /** PATCH 요청 진행 중이면 적용 버튼을 잠근다 */
  isApplying?: boolean;
  /** 서버가 거절한 사유(errorCode 로 만든 문구). 클라이언트 쪽 검증 오류와 같은 자리에 띄운다 */
  applyError?: string | null;
  onClose: () => void;
  onApply: (patch: {
    reviewEvent: boolean;
    /** 목록과 손님 화면에 보이는 대표 사진 한 장 */
    imageUrl: string | null;
    /** AI 대조용 표본 사진. 대표 사진과 별개다 */
    sampleUrls: (string | null)[];
  }) => void;
}
// FE-2.6: 가격은 이 모달에서 못 고친다 — 버튼처럼 보여도 클릭 안 되는 게 맞음
const lockedFieldClassName =
  'w-full cursor-not-allowed rounded-lg border border-neutral-200 bg-neutral-100 px-3 py-2 text-sm text-neutral-400';

export function MenuEditModal({
  menu,
  initialSampleUrls,
  isApplying = false,
  applyError = null,
  onClose,
  onApply,
}: MenuEditModalProps) {
  const { user } = useAuth();
  const [hasReviewEvent, setHasReviewEvent] = useState(menu.reviewEvent);
  // 대표 사진 — 목록과 손님 화면에 보이는 한 장. 서버의 menu_image_url 이다.
  const [imageUrl, setImageUrl] = useState(menu.imageUrl);
  // 표본 사진 5칸 — AI 대조 전용이라 대표 사진과 섞이지 않는다.
  const [sampleUrls, setSampleUrls] = useState<(string | null)[]>(() =>
    Array.from(
      { length: SAMPLE_IMAGE_COUNT },
      (_, i) => initialSampleUrls?.[i] ?? null,
    ),
  );
  const [error, setError] = useState<string | null>(null);

  const handleSampleChange = (index: number, url: string) => {
    setError(null);
    setSampleUrls((urls) => urls.map((u, i) => (i === index ? url : u)));
  };

  // 표본 사진이 한 장도 없으면 손님이 리뷰를 올려도 대조할 기준이 없다.
  // 리뷰이벤트를 끈 메뉴도 나중에 켤 수 있으므로 똑같이 요구한다.
  const handleApply = () => {
    if (sampleUrls.every((url) => url === null)) {
      setError('표본 사진을 한 장 이상 올려 주세요. 리뷰 사진을 대조할 기준이 됩니다.');
      return;
    }
    onApply({ reviewEvent: hasReviewEvent, imageUrl, sampleUrls });
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
          imageUrls={sampleUrls}
          onChange={handleSampleChange}
          onError={setError}
        />
        {/* 로그인한 사용자 이름(가게명) 표시 */}
        <div className="w-fit rounded-lg bg-neutral-200 px-4 py-2 text-sm font-semibold">
          {user?.displayName}
        </div>
        {/* 메뉴 썸네일 + 이름/가격 + 적용 버튼 */}
        <div className="flex items-center gap-4">
          {/* 대표 사진 — 위 표본 5칸과 별개다. 목록에 뜨는 것이 이 사진이다 */}
          <ImageUploadOverlay
            src={imageUrl}
            alt={menu.name}
            className="h-16 w-16 flex-shrink-0 rounded-lg bg-gray-200"
            onUploaded={(uploaded) => {
              setError(null);
              setImageUrl(uploaded.url);
            }}
            onError={setError}
          />
          <div className="flex-1">
            <div className="text-lg font-bold">{menu.name}</div>
            <div className="text-sm text-gray-600">{menu.price.toLocaleString('ko-KR')}원</div>
          </div>
          <Button size="small" onClick={handleApply} disabled={isApplying}>
            {isApplying ? '저장 중...' : '적용'}
          </Button>
        </div>
        {/* 메뉴 설정 — 이미지/가격은 읽기 전용*/}         
        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-neutral-600">메뉴 설정</span>
          
          <div className="flex flex-col gap-1">
            <span className="text-xs text-neutral-400">
              메뉴 이미지 — 메뉴 이름 옆 사진에 마우스를 올려 바꿔요. 메뉴판과
              주문 화면에 이 사진이 뜹니다.
            </span>
            <span className="text-xs text-neutral-400">
              표본 사진 — 맨 위 5칸. 손님이 올린 리뷰 사진을 이 사진들과 대조해
              판정해요. 여러 각도로 올릴수록 정확해집니다.
            </span>
            {(error || applyError) && (
              <span className="text-xs text-brand-900">{error ?? applyError}</span>
            )}
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
      </div>
    </div>
  );
}
