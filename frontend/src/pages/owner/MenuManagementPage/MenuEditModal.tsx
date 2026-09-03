import { useState } from 'react';
import { Button, ImageUploadOverlay } from '@/shared/ui';
import { useAuth } from '@/app/providers';
import { MenuSampleImages, SAMPLE_IMAGE_COUNT } from './MenuSampleImages';
import type { MenuItem } from '@/api/storeApi';

/**
 * 수정(edit)과 추가(create) 두 가지 모드로 사용된다.
 *
 *  - edit 모드(기본): 기존 메뉴를 menu props 로 받고 onApply 로 변경사항을 전달한다.
 *    이름·가격은 읽기 전용이다(서버 entity 설계상 생성 후 변경 불가).
 *  - create 모드(isNew=true): 빈 더미 menu 를 넘기고 onCreate 로 전체 데이터를 받는다.
 *    이름·가격이 입력 필드가 된다. 표본 사진은 edit 모드와 동일하게 최소 1장 필요 —
 *    백엔드 POST /api/stores/me/menus 가 생성 시점에도 requireValidSamples 를 적용한다.
 *
 * 두 모드가 같은 UI 레이아웃을 공유하므로 하나의 컴포넌트로 유지한다.
 * 별도 컴포넌트로 분리하면 isNew 분기가 두 파일에 흩어지는 대신,
 * 레이아웃 변경 시 동시에 두 파일을 수정해야 하는 문제가 생긴다.
 */
// FE-2.3: 메뉴 프로필 설정 / 리뷰 설정
interface MenuEditModalProps {
  menu: MenuItem;
  /** true 면 create 모드 — 이름·가격이 입력 필드가 되고 onCreate 가 필수다 */
  isNew?: boolean;
  /** 서버에 이미 저장돼 있던 표본 사진 */
  initialSampleUrls?: (string | null)[];
  /** PATCH/POST 요청 진행 중이면 버튼을 잠근다 */
  isApplying?: boolean;
  /** 서버가 거절한 사유(errorCode 로 만든 문구). 클라이언트 쪽 검증 오류와 같은 자리에 띄운다 */
  applyError?: string | null;
  onClose: () => void;
  /** edit 모드 전용: 이미지·표본 사진·리뷰이벤트 변경사항 전달 */
  onApply?: (patch: {
    reviewEvent: boolean;
    /** 목록과 손님 화면에 보이는 대표 사진 한 장 */
    imageUrl: string | null;
    /** AI 대조용 표본 사진. 대표 사진과 별개다 */
    sampleUrls: (string | null)[];
  }) => void;
  /** create 모드(isNew=true) 전용: 이름·가격을 포함한 전체 데이터 전달 */
  onCreate?: (data: {
    menuName: string;
    menuPrice: number;
    reviewEvent: boolean;
    imageUrl: string | null;
    sampleUrls: (string | null)[];
  }) => void;
}

// FE-2.6: 가격은 edit 모드에서 못 고친다 — 입력창처럼 보여도 읽기 전용이 맞음
const lockedFieldClassName =
  'w-full cursor-not-allowed rounded-lg border border-line-100 bg-fill-100 px-3 py-2 text-sm text-ink-500';

const editableFieldClassName =
  'w-full rounded-lg border border-line-100 bg-surface px-3 py-2 text-sm text-ink-900 focus:border-brand-800 focus:outline-none';

export function MenuEditModal({
  menu,
  isNew = false,
  initialSampleUrls,
  isApplying = false,
  applyError = null,
  onClose,
  onApply,
  onCreate,
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

  // create 모드에서만 사용하는 이름·가격 상태
  const [newName, setNewName] = useState('');
  const [newPrice, setNewPrice] = useState('');

  // 모달이 처음 열렸을 때(props)와 현재 편집 상태를 비교해 변경 여부를 판단한다.
  // create 모드에서는 이름 또는 가격이 입력됐으면 dirty 로 간주한다.
  const isDirty = isNew
    ? newName.trim() !== '' || newPrice !== ''
    : hasReviewEvent !== menu.reviewEvent ||
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

  const handleApply = () => {
    if (isNew) {
      // create 모드: 이름·가격 유효성 검사 후 onCreate 호출
      const price = parseInt(newPrice, 10);
      if (!newName.trim()) {
        setError('메뉴 이름을 입력해 주세요.');
        return;
      }
      if (isNaN(price) || price < 0) {
        setError('올바른 가격을 입력해 주세요.');
        return;
      }
      // edit 모드와 동일 — 백엔드 POST 도 requireValidSamples 적용
      if (sampleUrls.every((url) => url === null)) {
        setError('표본 사진을 한 장 이상 올려 주세요. 리뷰 사진을 대조할 기준이 됩니다.');
        return;
      }
      onCreate?.({ menuName: newName.trim(), menuPrice: price, reviewEvent: hasReviewEvent, imageUrl, sampleUrls });
    } else {
      // edit 모드: 표본 사진이 한 장도 없으면 손님이 리뷰를 올려도 대조할 기준이 없다.
      // 리뷰이벤트를 끈 메뉴도 나중에 켤 수 있으므로 똑같이 요구한다.
      if (sampleUrls.every((url) => url === null)) {
        setError('표본 사진을 한 장 이상 올려 주세요. 리뷰 사진을 대조할 기준이 됩니다.');
        return;
      }
      onApply?.({ reviewEvent: hasReviewEvent, imageUrl, sampleUrls });
    }
  };

  return (
    <>
    {/* 모달 오버레이 — 변경 없으면 즉시 닫힘, 있으면 확인 다이얼로그 표시 */}
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-6"
      onClick={handleOverlayClick}
    >
      {/* 모달 본문 */}
      <div
        className="flex w-full max-w-2xl flex-col gap-8 rounded-2xl bg-surface p-8 shadow-sheet"
        onClick={(e) => e.stopPropagation()}
      >
        <MenuSampleImages
          imageUrls={sampleUrls}
          onChange={handleSampleChange}
          onError={setError}
        />
        {/* 로그인한 사용자 이름(가게명) 표시 */}
        <div className="w-fit rounded-lg bg-fill-100 px-3 py-2 text-sm font-semibold text-ink-900">
          {user?.displayName}
        </div>
        {/* 메뉴 썸네일 + 이름/가격 + 적용(추가) 버튼 */}
        <div className="flex items-center gap-4">
          {/* 대표 사진 — 위 표본 5칸과 별개다. 목록에 뜨는 것이 이 사진이다.
              create 모드에서는 빈 자리만 표시하고 추가 후 수정에서 등록한다. */}
          <ImageUploadOverlay
            src={imageUrl}
            alt={isNew ? '' : menu.name}
            className="size-16 shrink-0 rounded-xl bg-fill-100"
            onUploaded={(uploaded) => {
              setError(null);
              setImageUrl(uploaded.url);
            }}
            onError={setError}
          />
          <div className="flex-1">
            {isNew ? (
              <>
                {/* create 모드: 이름·가격을 직접 입력한다 */}
                <input
                  type="text"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="메뉴 이름"
                  className="w-full text-base font-bold text-ink-900 placeholder:font-normal placeholder:text-ink-300 focus:outline-none"
                />
                <div className="flex items-center gap-1">
                  <input
                    type="number"
                    value={newPrice}
                    onChange={(e) => setNewPrice(e.target.value)}
                    placeholder="0"
                    min={0}
                    className="w-24 text-sm text-ink-700 placeholder:text-ink-300 focus:outline-none"
                  />
                  <span className="text-sm text-ink-700">원</span>
                </div>
              </>
            ) : (
              <>
                <div className="text-base font-bold text-ink-900">{menu.name}</div>
                <div className="text-sm text-ink-700">{menu.price.toLocaleString('ko-KR')}원</div>
              </>
            )}
          </div>
          <Button size="small" onClick={handleApply} disabled={isApplying}>
            {isApplying
              ? (isNew ? '추가 중...' : '저장 중...')
              : (isNew ? '추가' : '적용')}
          </Button>
        </div>
        {/* 메뉴 설정 — 이미지는 hover 업로드, 가격은 edit 모드에서 읽기 전용 */}
        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-ink-900">메뉴 설정</span>

          <div className="flex flex-col gap-1">
            <span className="text-xs text-ink-500">
              메뉴 이미지 — 메뉴 이름 옆 사진에 마우스를 올려 바꿔요. 메뉴판과
              주문 화면에 이 사진이 뜹니다.
            </span>
            <span className="text-xs text-ink-500">
              표본 사진 — 맨 위 5칸. 손님이 올린 리뷰 사진을 이 사진들과 대조해
              판정해요. 여러 각도로 올릴수록 정확해집니다.
            </span>
            {isNew && (
              <span className="text-xs text-ink-500">
                이름과 가격은 추가 후 변경할 수 없습니다. 신중하게 입력해 주세요.
              </span>
            )}
            {(error || applyError) && (
              <span className="text-xs text-brand-900">{error ?? applyError}</span>
            )}
          </div>

          {isNew ? (
            // create 모드: 이름·가격 모두 입력 가능 (위 썸네일 행과 같은 상태를 공유)
            <>
              <div className="flex items-center gap-2">
                <span className="w-24 shrink-0 text-xs text-ink-500">이름</span>
                <input
                  type="text"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="메뉴 이름 입력"
                  className={`flex-1 ${editableFieldClassName}`}
                />
              </div>
              <div className="flex items-center gap-2">
                <span className="w-24 shrink-0 text-xs text-ink-500">가격</span>
                <input
                  type="number"
                  value={newPrice}
                  onChange={(e) => setNewPrice(e.target.value)}
                  placeholder="0"
                  min={0}
                  className={`flex-1 ${editableFieldClassName}`}
                />
                <span className="shrink-0 text-xs text-ink-500">원</span>
              </div>
            </>
          ) : (
            // edit 모드: 가격은 서버 entity 설계상 변경 불가 — 잠금 필드로 표시
            <div className="flex items-center gap-2">
              <span className="w-24 shrink-0 text-xs text-ink-500">가격</span>
              <div className={`flex-1 ${lockedFieldClassName}`}>{menu.price.toLocaleString('ko-KR')}</div>
              <span className="shrink-0 text-xs text-ink-500">원</span>
            </div>
          )}
        </div>

        {/* 리뷰 이벤트 설정 — 이미지+후기 / 설정안함 선택 */}
        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-ink-900">리뷰 이벤트 설정</span>
          <button
            type="button"
            onClick={() => setHasReviewEvent(true)}
            className={`rounded-lg border px-4 py-2 text-left text-sm ${
              hasReviewEvent
                ? 'border-brand-800 bg-brand-50 font-semibold text-brand-800'
                : 'border-line-100 text-ink-700 hover:bg-fill-100'
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
                : 'border-line-100 text-ink-700 hover:bg-fill-100'
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
          className="flex flex-col gap-4 rounded-2xl bg-surface p-5 shadow-sheet"
          onClick={(e) => e.stopPropagation()}
        >
          <p className="text-sm font-semibold text-ink-900">
            {isNew ? '입력 중인 내용이 저장되지 않습니다. 나가시겠어요?' : '수정 중인 내용이 저장되지 않습니다. 나가시겠어요?'}
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" size="small" onClick={() => setShowConfirm(false)}>
              {isNew ? '계속 입력' : '계속 수정'}
            </Button>
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
