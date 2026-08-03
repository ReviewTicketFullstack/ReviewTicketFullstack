import { Button } from '@/shared/ui';
import { store } from './mockData';

export function StoreManagementPage() {
  return (
    <div className="flex flex-col gap-4 p-6">
      <h1 className="text-xl font-bold text-ink-900">가게관리</h1>

      <div className="flex flex-col gap-4 rounded-lg bg-neutral-100 p-6">
        <div className="flex items-center gap-4">
          <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-lg bg-gray-200">
            <span className="text-xs text-gray-400">가게 로고</span>
          </div>
          <span className="flex-1 text-lg font-bold text-ink-900">{store.name}</span>
          <Button variant="secondary" size="small" disabled>
            수정
          </Button>
        </div>

        <div className="flex h-24 w-20 items-center justify-center rounded-lg bg-gray-200">
          <span className="text-xs text-gray-400">배경 사진</span>
        </div>
      </div>
    </div>
  );
}
