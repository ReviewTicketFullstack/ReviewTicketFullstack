import { Card } from '@/shared/ui';
import { StoreCard } from './StoreCard';

export function HomePage() {
  return (
    <div className="space-y-6 px-5 py-6">
      {/* Greeting Card Section */}
      <div className="grid grid-cols-2 gap-4">
        <Card className="flex items-center justify-center p-6">
          <div className="text-center">
            <p className="text-lg font-bold leading-7">안녕하세요?</p>
            <p className="text-lg font-bold leading-7">이도연님!</p>
          </div>
        </Card>
      </div>

      {/* Store List Section */}
      <div className="space-y-4">
        <StoreCard
          storeName="도미너피자"
          rating={4.7}
          reviewCount="100+"
        />
      </div>
    </div>
  );
}
