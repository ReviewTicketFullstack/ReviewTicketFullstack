package com.reviewticket.server.store;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.ForbiddenException;
import com.reviewticket.server.auth.NotFoundException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.Store;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.MenuRepository;
import com.reviewticket.server.repository.PendingSignupRepository;
import com.reviewticket.server.repository.StoreRepository;
import com.reviewticket.server.repository.UserRepository;

/**
 * 가게, 메뉴 조회와 생성.
 */
@Service
public class StoreService {

    private final StoreRepository stores;
    private final MenuRepository menus;
    private final UserRepository users;
    private final PendingSignupRepository pendings;

    public StoreService(StoreRepository stores, MenuRepository menus,
            UserRepository users, PendingSignupRepository pendings) {
        this.stores = stores;
        this.menus = menus;
        this.users = users;
        this.pendings = pendings;
    }

    /**
     * 프로토타입용 기본 메뉴. 프론트(OrderPage, MenuManagementPage)가 이미 이
     * 5종을 화면에 고정해 두고 있어 이름과 리뷰이벤트 여부를 거기 맞춘다.
     *
     * 메뉴관리 API가 붙으면 이 상수와 아래 생성 코드를 지운다.
     */
    private record SeedMenu(String name, int price, boolean reviewEvent) {
    }

    /** 메뉴 하나가 가질 수 있는 표본 사진 수. 프론트 SAMPLE_IMAGE_COUNT 와 같은 값이다. */
    private static final int MAX_SAMPLE_IMAGES = 5;

    private static final List<SeedMenu> SEED_MENUS = List.of(
            new SeedMenu("피자", 18000, true),
            new SeedMenu("햄버거", 9000, true),
            new SeedMenu("치킨윙", 15000, false),
            new SeedMenu("비빔밥", 10000, false),
            new SeedMenu("라멘", 11000, false));

    /**
     * 사장 회원가입이 확정될 때 가게와 기본 메뉴를 함께 만든다.
     *
     * 이미 가게가 있으면 아무것도 하지 않는다 — 가입 확정은 한 번뿐이지만,
     * 이 메서드가 다른 경로에서 다시 불려도 가게가 둘로 늘지 않아야 한다.
     */
    @Transactional
    public void createForOwner(User owner) {
        if (stores.existsByOwner(owner)) {
            return;
        }
        Store store = stores.save(new Store(owner, owner.getDisplayName()));
        menus.saveAll(SEED_MENUS.stream()
                .map(seed -> new Menu(store, seed.name(), seed.price(), null, seed.reviewEvent()))
                .toList());

        // review_event 대상 메뉴가 하나라도 있는지 여기서 한 번만 계산해 둔다.
        // 메뉴 수정 API가 없어 이후로는 바뀔 일이 없다.
        boolean reviewing = menus.existsByStoreIdAndReviewEventTrue(store.getId());
        store.markReviewing(reviewing);
    }

    /** 홈 목록. 최신 가게가 먼저 온다. */
    @Transactional(readOnly = true)
    public List<StoreSummaryResponse> findAll() {
        return stores.findAllByOrderByIdDesc().stream()
                .map(StoreService::toSummary)
                .toList();
    }

    /** 주문 화면용 상세. 가게 정보에 그 가게 메뉴를 붙여 돌려준다. */
    @Transactional(readOnly = true)
    public StoreDetailResponse findById(Long storeId) {
        Store store = loadStore(storeId);
        List<MenuResponse> menuList = menus.findByStoreIdOrderByIdAsc(storeId).stream()
                .map(StoreService::toMenuResponse)
                .toList();

        return new StoreDetailResponse(store.getId(), store.getName(), store.getLogoUrl(),
                store.getReviewNumber(), store.getReviewValue(), store.isReviewing(), menuList);
    }

    /** GET /api/stores/me. */
    @Transactional(readOnly = true)
    public StoreMeResponse findMine(long userId) {
        Store store = storeOf(requireOwner(userId));
        return toMeResponse(store);
    }

    /** GET /api/stores/me/menus. */
    @Transactional(readOnly = true)
    public List<MenuOwnerResponse> findMyMenus(long userId) {
        Store store = storeOf(requireOwner(userId));
        return menus.findByStoreIdOrderByIdAsc(store.getId()).stream()
                .map(StoreService::toMenuOwnerResponse)
                .toList();
    }

    /**
     * PATCH /api/stores/me. 이름과 로고를 통째로 덮어쓴다.
     *
     * 가게 이름이 바뀌면 users.display_name 도 같은 값으로 함께 바꾼다 — 둘은
     * 하나의 이름을 store_table 과 users 두 표에 나눠 적어 둔 것일 뿐이다.
     * 반대 방향(계정 API 로 이름 변경)은 AccountService 가 사장 계정을 거절해
     * 이쪽으로만 실제로 바뀌게 되어 있다.
     */
    @Transactional
    public StoreMeResponse updateMine(long userId, String rawName, String logoUrl) {
        User owner = requireOwner(userId);
        Store store = storeOf(owner);

        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new ValidationException("NAME_REQUIRED", "가게 이름을 입력해 주세요");
        }
        if (name.length() > 32) {
            throw new ValidationException("NAME_TOO_LONG", "이름은 32자 이하여야 합니다");
        }

        if (!name.equals(store.getName())) {
            // AccountService.changeDisplayName 과 같은 기준 — 가입 대기 중인 이름도
            // 예약된 것으로 본다.
            if (users.existsByDisplayName(name) || pendings.existsByDisplayName(name)) {
                throw new ConflictException("NAME_TAKEN", "이미 쓰이고 있는 가게 이름입니다");
            }
            owner.changeDisplayName(name);
        }
        store.changeInfo(name, logoUrl);

        return toMeResponse(store);
    }

    /**
     * PATCH /api/stores/me/menus/{menuId}. 대표 사진·표본 사진·리뷰이벤트 여부를
     * 통째로 덮어쓴다.
     *
     * 메뉴 번호가 다른 가게 것이어도 STORE_NOT_FOUND 와 같은 이유로 404 하나로
     * 묶는다 — 남의 메뉴 번호를 넣어 봐도 그게 존재하는지조차 알 수 없게 한다.
     *
     * reviewEvent 가 바뀌면 가게의 "리뷰 진행중" 표시(store.reviewing)도 다시
     * 계산해야 한다. 메뉴 수정 API가 없던 때는 가게 생성 시 한 번만 계산하면
     * 됐지만, 이제는 메뉴가 바뀔 때마다 다시 봐야 한다.
     */
    @Transactional
    public MenuOwnerResponse updateMyMenu(long userId, Long menuId, String imageUrl,
            List<String> sampleImageUrls, boolean reviewEvent) {
        Store store = storeOf(requireOwner(userId));
        Menu menu = menus.findById(menuId)
                .orElseThrow(() -> new NotFoundException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다"));
        if (!menu.getStore().getId().equals(store.getId())) {
            throw new NotFoundException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다");
        }

        List<String> samples = sampleImageUrls == null ? List.of() : sampleImageUrls;
        if (samples.size() > MAX_SAMPLE_IMAGES) {
            throw new ValidationException("TOO_MANY_SAMPLE_IMAGES",
                    "표본 사진은 " + MAX_SAMPLE_IMAGES + "장까지만 등록할 수 있습니다");
        }
        if (samples.stream().noneMatch(Objects::nonNull)) {
            throw new ValidationException("SAMPLE_IMAGE_REQUIRED", "표본 사진을 한 장 이상 등록해야 합니다");
        }

        menu.applyEdit(imageUrl, samples, reviewEvent);
        store.markReviewing(menus.existsByStoreIdAndReviewEventTrue(store.getId()));

        return toMenuOwnerResponse(menu);
    }

    /** 사장 전용 기능을 지키는 공통 관문. 고객 계정이면 403, 못 찾으면 401. */
    private User requireOwner(long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "로그인이 필요합니다"));
        if (user.getRole() != Role.OWNER) {
            throw new ForbiddenException("NOT_OWNER", "고객 계정으로 사장 전용 기능을 불렀습니다");
        }
        return user;
    }

    private Store storeOf(User owner) {
        return stores.findByOwner(owner)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "가게 행이 없습니다"));
    }

    /** 번호로 가게를 찾는다. 없으면 404. */
    public Store loadStore(Long storeId) {
        return stores.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "가게를 찾을 수 없습니다"));
    }

    /** ReviewService 등 다른 서비스가 "이 사람이 사장이고, 그 가게가 이거다"를 한 번에 확인할 때 쓴다. */
    public Store requireOwnerStore(long userId) {
        return storeOf(requireOwner(userId));
    }

    private static StoreSummaryResponse toSummary(Store store) {
        return new StoreSummaryResponse(store.getId(), store.getName(), store.getLogoUrl(),
                store.getReviewNumber(), store.getReviewValue(), store.isReviewing());
    }

    private static StoreMeResponse toMeResponse(Store store) {
        return new StoreMeResponse(store.getId(), store.getOwner().getId(), store.getName(),
                store.getLogoUrl(), store.getReviewNumber(), store.getReviewValue(), store.isReviewing(),
                toUtc(store.getCreatedAt()), toUtc(store.getLatestUpdate()));
    }

    private static MenuResponse toMenuResponse(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getName(), menu.getPrice(),
                menu.getImageUrl(), menu.isReviewEvent());
    }

    private static MenuOwnerResponse toMenuOwnerResponse(Menu menu) {
        return new MenuOwnerResponse(menu.getStore().getId(), menu.getId(), menu.getName(), menu.getPrice(),
                menu.getImageUrl(), menu.getSampleImageUrls(), menu.isReviewEvent(),
                toUtc(menu.getCreatedAt()), toUtc(menu.getLatestUpdate()));
    }

    /** 저장된 값은 서버 시간대(LocalDateTime)라, 기준이 분명한 UTC(끝에 Z)로 바꿔서 보낸다. */
    static Instant toUtc(LocalDateTime serverTime) {
        return serverTime == null ? null : serverTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
