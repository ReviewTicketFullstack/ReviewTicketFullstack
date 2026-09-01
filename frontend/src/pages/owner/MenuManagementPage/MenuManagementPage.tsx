import { useEffect, useState } from 'react';
import { Button, Card, EmptyState, Loading } from '@/shared/ui';
import { useAuth } from '@/app/providers';
import { ApiError } from '@/shared/api';
import { MenuListItem } from './MenuListItem';
import { MenuEditModal } from './MenuEditModal';
import { getMyMenus, updateMyMenu, createMyMenu, type MenuItem } from '@/api/storeApi';

/** 서버 errorCode → 화면 문구. 백엔드는 message 를 보내지 않으므로 여기서 채운다. */
function toMenuError(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback;
  switch (error.errorCode) {
    case 'MENU_NAME_REQUIRED':    return '메뉴 이름을 입력해 주세요.';
    case 'MENU_NAME_TOO_LONG':    return '메뉴 이름은 32자 이하여야 합니다.';
    case 'MENU_PRICE_INVALID':    return '가격은 0원 이상이어야 합니다.';
    case 'MENU_NAME_TAKEN':       return '이미 있는 메뉴 이름입니다. 다른 이름을 써 주세요.';
    case 'SAMPLE_IMAGE_REQUIRED': return '표본 사진을 한 장 이상 등록해야 합니다.';
    case 'TOO_MANY_SAMPLE_IMAGES':return '표본 사진은 5장까지만 등록할 수 있습니다.';
    case 'SAMPLE_IMAGE_NOT_FOUND':return '표본 사진을 찾을 수 없습니다. 다시 올려 주세요.';
    case 'SAMPLE_IMAGE_TOO_SMALL':return '표본 사진은 긴 쪽이 1920px 이상이어야 합니다.';
    default:                       return fallback;
  }
}

/** 목록·모달이 함께 쓰는 형태. 표본 사진은 목록에 안 뜨지만 모달이 다시 열릴 때 필요하다. */
type OwnerMenuItem = MenuItem & { sampleImageUrls: (string | null)[] };

export function MenuManagementPage() {
  const { user } = useAuth();
  const [menuItems, setMenuItems] = useState<OwnerMenuItem[]>([]);
  // 클릭한 메뉴 — 있으면 수정 모달이 열림
  const [selectedMenu, setSelectedMenu] = useState<OwnerMenuItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [applyError, setApplyError] = useState<string | null>(null);
  const [isApplying, setIsApplying] = useState(false);
  // 메뉴 추가 모달 표시 여부
  const [showCreateModal, setShowCreateModal] = useState(false);

  // 내 가게 메뉴 조회. 가게 번호를 보내지 않는다 — 토큰의 주체가 곧 그 가게의 사장이다.
  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      try {
        const menus = await getMyMenus(controller.signal);
        // 사장용 응답은 필드명 앞에 menu 가 붙고 시각까지 실려 온다.
        // 목록 컴포넌트가 쓰는 형태로 옮겨 담는다.
        setMenuItems(
          menus.map((menu) => ({
            id: menu.menuId,
            name: menu.menuName,
            price: menu.menuPrice,
            imageUrl: menu.menuImageUrl,
            sampleImageUrls: menu.sampleImageUrls,
            reviewEvent: menu.reviewEvent,
          })),
        );
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setError('메뉴를 불러오지 못했습니다.');
      } finally {
        setIsLoading(false);
      }
    }

    load();
    return () => controller.abort();
  }, [user?.displayName]);

  // 모달의 "적용" 클릭 시: PATCH /api/stores/me/menus/{id} 로 저장하고,
  // 서버가 돌려준 값으로 목록을 갱신한다 — 저장 지점을 여기 하나로 모아 둔다.
  const handleApply = async (patch: {
    reviewEvent: boolean;
    imageUrl: string | null;
    sampleUrls: (string | null)[];
  }) => {
    if (!selectedMenu) return;
    setIsApplying(true);
    setApplyError(null);
    try {
      const updated = await updateMyMenu(selectedMenu.id, {
        imageUrl: patch.imageUrl,
        sampleImageUrls: patch.sampleUrls,
        reviewEvent: patch.reviewEvent,
      });
      setMenuItems((items) =>
        items.map((item) =>
          item.id === selectedMenu.id
            ? {
                ...item,
                imageUrl: updated.menuImageUrl,
                sampleImageUrls: updated.sampleImageUrls,
                reviewEvent: updated.reviewEvent,
              }
            : item,
        ),
      );
      setSelectedMenu(null);
    } catch (err) {
      setApplyError(toMenuError(err, '메뉴를 저장하지 못했습니다.'));
    } finally {
      setIsApplying(false);
    }
  };

  // MenuEditModal(isNew=true) 의 onCreate 핸들러.
  // 서버에 POST 요청 후 응답 데이터로 목록을 낙관적 업데이트한다.
  // 백엔드 API 가 붙으면 name·price 외 imageUrl·sampleUrls·reviewEvent 도
  // 한 번에 전달할 수 있도록 createMyMenu 시그니처를 확장하면 된다.
  const handleCreate = async (data: {
    menuName: string;
    menuPrice: number;
    reviewEvent: boolean;
    imageUrl: string | null;
    sampleUrls: (string | null)[];
  }) => {
    setIsApplying(true);
    setApplyError(null);
    try {
      const created = await createMyMenu(
        data.menuName,
        data.menuPrice,
        data.imageUrl,
        data.sampleUrls,
        data.reviewEvent,
      );
      setMenuItems((items) => [
        ...items,
        {
          id: created.menuId,
          name: created.menuName,
          price: created.menuPrice,
          imageUrl: created.menuImageUrl,
          sampleImageUrls: created.sampleImageUrls,
          reviewEvent: created.reviewEvent,
        },
      ]);
      setShowCreateModal(false);
    } catch (err) {
      setApplyError(toMenuError(err, '메뉴를 추가하지 못했습니다.'));
    } finally {
      setIsApplying(false);
    }
  };

  return (
    <div className="flex flex-col gap-5 px-5 py-8">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-xl font-bold text-ink-900">메뉴관리</h1>
        <Button size="medium" onClick={() => setShowCreateModal(true)}>
          메뉴 추가
        </Button>
      </div>

      {isLoading && <Loading />}
      {error && <p className="text-sm text-brand-900">{error}</p>}

      {!isLoading && !error && menuItems.length === 0 && (
        <EmptyState
          icon="🍽️"
          message="아직 등록된 메뉴가 없어요. 메뉴 추가를 눌러 첫 메뉴를 만들어 보세요."
        />
      )}

      {!isLoading && !error && menuItems.length > 0 && (
        <Card className="overflow-hidden p-0">
          <ul className="divide-y divide-line-100">
            {menuItems.map((menu) => (
              <li key={menu.id}>
                <MenuListItem menu={menu} onClick={() => setSelectedMenu(menu)} />
              </li>
            ))}
          </ul>
        </Card>
      )}

      {/* 메뉴 클릭 시에만 수정 모달 표시 */}
      {selectedMenu && (
        <MenuEditModal
          menu={selectedMenu}
          initialSampleUrls={selectedMenu.sampleImageUrls}
          isApplying={isApplying}
          applyError={applyError}
          onClose={() => setSelectedMenu(null)}
          onApply={handleApply}
        />
      )}

      {/* create 모드: 빈 더미 menu 를 넘기고 onCreate 로 API 호출 후 목록 갱신 */}
      {showCreateModal && (
        <MenuEditModal
          isNew
          menu={{ id: 0, name: '', price: 0, imageUrl: null, reviewEvent: false }}
          isApplying={isApplying}
          applyError={applyError}
          onClose={() => setShowCreateModal(false)}
          onCreate={handleCreate}
        />
      )}
    </div>
  );
}
