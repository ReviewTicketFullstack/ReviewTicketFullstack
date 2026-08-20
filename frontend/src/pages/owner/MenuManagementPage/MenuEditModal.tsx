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
// FE-2.6: 가격은 이 모달에서 못 고친다 — 입력창처럼 보여도 읽기 전용이 맞음
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
  // 오버레이 클릭 시 변경사항이 있으면 바로 닫지 않고 이 확인 다이얼로그를 먼저 띄운다.
  const [showConfirm, setShowConfirm] = useState(false);

  // 모달이 처음 열렸을 때(props)와 현재 편집 상태를 비교해 변경 여부를 판단한다.
  // 리뷰이벤트 설정, 대표 사진, 표본 사진 중 하나라도 바뀌면 dirty로 간주한다.
  const isDirty =
    hasReviewEvent !== menu.reviewEvent ||
    imageUrl !== menu.imageUrl ||
    sampleUrls.some((url, i) => url !== (initialSampleUrls?.[i] ?? null));

  // 오버레이(배경) 클릭 핸들러.
  // dirty 상태면 확인 다이얼로그를 띄우고, 변경사항이 없으면 바로 닫는다.
  const handleOverlayClick = () => {
    if (isDirty) {
      setShowConfirm(true);
    } else {
      onClose();
    }
  };

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
    <>
    {/* 모달 오버레이 — 변경 없으면 즉시 닫힘, 있으면 확인 다이얼로그 표시 */}
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-6"
      onClick={handleOverlayClick}
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
        {/* 메뉴 설정 — 이미지는 hover 업로드, 가격은 읽기 전용 */}
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

    {/* 변경사항이 있을 때 오버레이 클릭 시 나타나는 이탈 확인 다이얼로그.
        z-[60]으로 기존 모달(z-50) 위에 렌더링된다.
        다이얼로그 배경 클릭 시 다이얼로그만 닫히고 편집 모달은 유지된다. */}
    {showConfirm && (
      <div
        className="fixed inset-0 z-[60] flex items-center justify-center bg-black/30"
        onClick={() => setShowConfirm(false)}
      >
        <div
          className="flex flex-col gap-4 rounded-xl bg-white p-6 shadow-xl"
          onClick={(e) => e.stopPropagation()}
        >
          <p className="text-sm font-semibold text-ink-900">
            수정 중인 내용이 저장되지 않습니다. 나가시겠어요?
          </p>
          <div className="flex justify-end gap-2">
            {/* 계속 수정: 확인 다이얼로그만 닫고 편집 모달로 돌아간다 */}
            <Button variant="secondary" size="small" onClick={() => setShowConfirm(false)}>
              계속 수정
            </Button>
            {/* 나가기: 변경사항을 버리고 편집 모달을 완전히 닫는다 */}
            <Button size="small" onClick={onClose}>
              나가기
            </Button>
          </div>
        </div>
      </div>
    )}
    </>
  );
}
