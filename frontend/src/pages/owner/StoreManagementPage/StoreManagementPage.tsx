import { useState } from 'react';
import { Button } from '@/shared/ui';
import { useAuth } from '@/app/providers';

export function StoreManagementPage() {
  const { user, updateDisplayName } = useAuth();
  // 보기 모드 / 편집 모드 전환 플래그
  const [isEditing, setIsEditing] = useState(false);
  // input에 바인딩되는 임시 값 — 저장 누르기 전까진 user.displayName을 안 건드림
  const [name, setName] = useState(user?.displayName ?? '');

  // 저장 버튼 클릭 시: AuthProvider의 user.displayName을 갱신하고 편집 모드 종료
  // (로컬 state만 바뀜, 새로고침하면 로그인 응답값으로 되돌아감 — 아직 서버 API 없음)
  const handleSave = () => {
    updateDisplayName(name);
    setIsEditing(false);
  };

  return (
    <div className="flex flex-col gap-4 p-6">
      <h1 className="text-xl font-bold text-ink-900">가게관리</h1>

      <div className="flex flex-col gap-4 rounded-lg bg-neutral-100 p-6">
        <span className="text-sm font-semibold text-neutral-600">가게 정보</span>

        <div className="flex items-center gap-4">
          {/* 가게 로고 자리 — 실제 이미지 URL 없어서 회색 placeholder */}
          <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-lg bg-gray-200">
            <span className="text-xs text-gray-400">가게 로고</span>
          </div>

          {isEditing ? (
            <>
              {/* 편집 모드: 가게명 입력창 */}
              <input
                className="flex-1 rounded border border-neutral-300 px-2 py-1 text-lg font-bold text-ink-900"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
              {/* 저장 버튼 — 클릭 시 handleSave 실행 */}
              <Button variant="secondary" size="small" onClick={handleSave}>
                저장
              </Button>
            </>
          ) : (
            <>
              {/* 보기 모드: 현재 가게명 텍스트 */}
              <span className="flex-1 text-lg font-bold text-ink-900">{user?.displayName}</span>
              {/* 수정 버튼 — 클릭 시 편집 모드로 전환 */}
              <Button variant="secondary" size="small" onClick={() => setIsEditing(true)}>
                수정
              </Button>
            </>
          )}
        </div>

        <span className="text-sm font-semibold text-neutral-600">배경 사진</span>
        {/* 배경 사진 자리 — 아직 업로드 기능 없음, placeholder만 표시 */}
        <div className="flex h-24 w-20 items-center justify-center rounded-lg bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>

      </div>
    </div>
  );
}
