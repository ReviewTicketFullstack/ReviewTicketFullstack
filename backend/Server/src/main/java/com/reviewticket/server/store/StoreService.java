package com.reviewticket.server.store;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Store;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.MenuRepository;
import com.reviewticket.server.repository.StoreRepository;

/**
 * 가게 조회와 생성.
 *
 * rating 과 reviewCount 는 리뷰 표가 아직 없어 0 으로 내려간다. 화면은 이
 * 값으로도 정상 동작하고, 리뷰 기능이 붙으면 이 서비스에서 계산해 채운다.
 */
@Service
public class StoreService {

    private final StoreRepository stores;
    private final MenuRepository menus;

    public StoreService(StoreRepository stores, MenuRepository menus) {
        this.stores = stores;
        this.menus = menus;
    }

    /**
     * 사장 회원가입이 확정될 때 가게를 함께 만든다.
     *
     * 이미 가게가 있으면 아무것도 하지 않는다 — 가입 확정은 한 번뿐이지만,
     * 이 메서드가 다른 경로에서 다시 불려도 가게가 둘로 늘지 않아야 한다.
     */
    @Transactional
    public void createForOwner(User owner) {
        if (stores.existsByOwner(owner)) {
            return;
        }
        stores.save(new Store(owner, owner.getDisplayName()));
    }

    /** 홈 목록. 최신 가게가 먼저 온다. */
    @Transactional(readOnly = true)
    public List<StoreSummaryResponse> findAll() {
        List<Store> found = stores.findAllByOrderByIdDesc();
        if (found.isEmpty()) {
            return List.of();
        }

        // 가게마다 메뉴를 따로 조회하면 목록 길이만큼 쿼리가 늘어난다(N+1).
        // 배지가 붙는 가게 id 만 한 번에 받아 두고 메모리에서 맞춘다.
        List<Long> ids = found.stream().map(Store::getId).toList();
        Set<Long> withEvent = new HashSet<>(menus.findStoreIdsWithReviewEvent(ids));

        return found.stream()
                .map(store -> new StoreSummaryResponse(
                        store.getId(),
                        store.getName(),
                        store.getImageUrl(),
                        0.0,
                        0,
                        withEvent.contains(store.getId())))
                .toList();
    }

    /** 주문 화면용 상세. 가게 정보에 그 가게 메뉴를 붙여 돌려준다. */
    @Transactional(readOnly = true)
    public StoreDetailResponse findById(Long storeId) {
        Store store = stores.findById(storeId)
                .orElseThrow(() -> new ValidationException("STORE_NOT_FOUND", "가게를 찾을 수 없습니다"));

        List<MenuResponse> menuList = menus.findByStoreIdOrderByIdAsc(storeId).stream()
                .map(StoreService::toMenuResponse)
                .toList();

        return new StoreDetailResponse(
                store.getId(),
                store.getName(),
                store.getImageUrl(),
                0.0,
                0,
                menuList);
    }

    private static MenuResponse toMenuResponse(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getImageUrl(),
                menu.isReviewEvent());
    }
}
