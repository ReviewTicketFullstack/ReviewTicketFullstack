import { useEffect, useRef, useState } from "react";
import { Button } from "@/shared/ui";
import { useAuth } from "@/app/providers";
import { useStoreLogo } from "@/shared/layout/OwnerLayout/StoreLogoContext";
import { getMyStore, updateMyStore } from "@/api/storeApi";
import { uploadImage } from "@/api/uploadApi";
import { ApiError } from "@/shared/api";

export function StoreManagementPage() {
  const { user, updateDisplayName } = useAuth();
  const { logo, setLogo } = useStoreLogo();
  const [isEditing, setIsEditing] = useState(false);
  // 서버가 알고 있는 가게 이름. users.display_name 이 아니라 store_table 의 값이 정답이다.
  const [storeName, setStoreName] = useState("");
  const [name, setName] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 가게 정보 조회. 가게 번호를 보내지 않는다 — 토큰의 주체가 곧 그 가게의 사장이다.
  useEffect(() => {
    const controller = new AbortController();

    getMyStore(controller.signal)
      .then((store) => {
        setStoreName(store.storeName);
        setName(store.storeName);
        if (store.logoUrl) setLogo(store.logoUrl);
      })
      .catch((err) => {
        if (err instanceof DOMException && err.name === "AbortError") return;
        setErrorMessage("가게 정보를 불러오지 못했습니다.");
      });

    return () => controller.abort();
  }, [setLogo]);

  // 편집모드 진입: 현재 이름으로 입력 초기화
  const handleStartEdit = () => {
    setName(storeName);
    setErrorMessage(null);
    setIsEditing(true);
  };

  // 취소: 편집모드만 종료, 이름 변경은 버림 (로고는 이미 반영된 채로 유지)
  const handleCancel = () => {
    setErrorMessage(null);
    setIsEditing(false);
  };

  /**
   * 저장: PATCH /api/stores/me 호출. 이름과 로고를 통째로 덮어쓴다.
   *
   * PATCH /api/me/name 은 쓰지 않는다 — 그쪽은 고객 전용이라 사장이 부르면
   * 403 NOT_CUSTOMER 로 거절당한다. 가게 이름의 정답은 store_table 에 있다.
   */
  const handleSave = async () => {
    setIsSaving(true);
    setErrorMessage(null);
    try {
      const updated = await updateMyStore(name, logo);
      setStoreName(updated.storeName);
      // 서버가 users.display_name 도 같은 값으로 맞춰 주므로 화면 상태도 함께 갱신한다.
      updateDisplayName(updated.storeName);
      setIsEditing(false);
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : "저장에 실패했습니다.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  // 파일 선택 즉시 서버에 올리고 돌려받은 주소를 미리보기에 반영한다.
  // 실제 가게 정보에 붙는 것은 위 "수정"을 눌러 저장할 때다.
  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (fileInputRef.current) fileInputRef.current.value = "";
    if (!file || !file.type.startsWith("image/")) return;

    setErrorMessage(null);
    try {
      const uploaded = await uploadImage(file);
      setLogo(uploaded.url);
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : "이미지를 올리지 못했습니다.",
      );
    }
  };

  return (
    <div className="flex flex-col gap-4 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-ink-900">가게관리</h1>
        {isEditing ? (
          <div className="flex gap-2">
            <Button
              variant="secondary"
              size="small"
              onClick={handleSave}
              disabled={isSaving}
            >
              수정
            </Button>
            <Button
              variant="secondary"
              size="small"
              onClick={handleCancel}
              disabled={isSaving}
            >
              취소
            </Button>
          </div>
        ) : (
          <Button variant="secondary" size="small" onClick={handleStartEdit}>
            정보 수정
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-4 rounded-lg bg-neutral-100 p-6">
        <span className="text-sm font-semibold text-neutral-600">
          가게 정보
        </span>

        <div className="flex items-start gap-4">
          <div className="flex flex-col gap-1">
            {/* 가게 로고 — 확정된 이미지 있으면 표시, 없으면 placeholder */}
            {logo ? (
              <img
                src={logo}
                alt="가게 로고"
                className="h-20 w-20 flex-shrink-0 rounded-lg object-cover"
              />
            ) : (
              <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-lg bg-gray-200">
                <span className="text-xs text-gray-400">가게 로고</span>
              </div>
            )}
            {isEditing && (
              <>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleFileSelected}
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="text-xs text-neutral-600 underline"
                >
                  이미지등록
                </button>
              </>
            )}
          </div>

          {isEditing ? (
            <div className="flex flex-1 flex-col gap-1 self-center">
              <div className="flex items-center gap-2">
                <input
                  className="flex-1 rounded border border-neutral-300 px-2 py-1 text-lg font-bold text-ink-900"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
                {/* 별도 사전 중복확인 엔드포인트는 없음 — 중복 검사는 우측 상단 "수정" 저장 시 서버가 함께 처리 */}
                <Button variant="secondary" size="small" disabled>
                  확인
                </Button>
              </div>
              {errorMessage && (
                <span className="text-xs text-brand-900">{errorMessage}</span>
              )}
            </div>
          ) : (
            <span className="flex-1 self-center text-lg font-bold text-ink-900">
              {storeName || user?.displayName}
            </span>
          )}
        </div>

        <span className="text-sm font-semibold text-neutral-600">
          배경 사진
        </span>
        {/* 배경 사진 — 편집 기능 미구현, 그대로 placeholder */}
        <div className="flex h-24 w-20 items-center justify-center rounded-lg bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>
      </div>
    </div>
  );
}
