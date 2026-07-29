import { DetailStoreCard } from './DetailStoreCard';

export function OrderPage() {
  return (
    <div className="space-y-6 px-5 py-6">
      <div>
        <h1 className="text-2xl font-bold">메뉴</h1>
        <p className="text-gray-600">메뉴확인</p>
      </div>

      {/* Store Promotion Card Section */}
      <DetailStoreCard
        storeName="도미너피자"
        rating={4.7}
        reviewCount="150"
      />
    </div>
  );
}
