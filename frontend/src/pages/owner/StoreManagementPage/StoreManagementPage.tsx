import { Button } from '@/shared/ui';
import { useAuth } from '@/app/providers';

export function StoreManagementPage() {
  const { user } = useAuth();

  return (
    <div className="flex flex-col gap-4 p-6">
      <h1 className="text-xl font-bold text-ink-900">가게관리</h1>

      <div className="flex flex-col gap-4 rounded-lg bg-neutral-100 p-6">
        <span className="text-sm font-semibold text-neutral-600">가게 정보</span>

        <div className="flex items-center gap-4">
          <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-lg bg-gray-200">
            <span className="text-xs text-gray-400">가게 로고</span>
          </div>
          <span className="flex-1 text-lg font-bold text-ink-900">{user?.displayName}</span>
          <Button variant="secondary" size="small" disabled>
            수정
          </Button>
        </div>

        <span className="text-sm font-semibold text-neutral-600">배경 사진</span>
        <div className="flex h-24 w-20 items-center justify-center rounded-lg bg-gray-200">
          <span className="text-xs text-gray-400">Image</span>
        </div>

      </div>
    </div>
  );
}
