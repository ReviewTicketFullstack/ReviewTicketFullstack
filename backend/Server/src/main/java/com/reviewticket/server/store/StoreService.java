package com.reviewticket.server.store;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.ForbiddenException;
import com.reviewticket.server.auth.NotFoundException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.Store;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.image.ImageResizer;
import com.reviewticket.server.image.ImageStorage;
import com.reviewticket.server.repository.MenuRepository;
import com.reviewticket.server.repository.PendingSignupRepository;
import com.reviewticket.server.repository.StoreRepository;
import com.reviewticket.server.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * 가게, 메뉴 조회와 생성.
 */
@Service
public class StoreService {

    private final StoreRepository stores;
    private final MenuRepository menus;
    private final UserRepository users;
    private final PendingSignupRepository pendings;
    private final EntityManager entityManager;
    private final ImageStorage storage;
    private final ImageResizer resizer;
    private final ReviewTicketProperties properties;

    public StoreService(StoreRepository stores, MenuRepository menus,
            UserRepository users, PendingSignupRepository pendings, EntityManager entityManager,
            ImageStorage storage, ImageResizer resizer, ReviewTicketProperties properties) {
        this.stores = stores;
        this.menus = menus;
        this.users = users;
        this.pendings = pendings;
        this.entityManager = entityManager;
        this.storage = storage;
        this.resizer = resizer;
        this.properties = properties;
    }

    /** 메뉴 하나가 가질 수 있는 표본 사진 수. 프론트 SAMPLE_IMAGE_COUNT 와 같은 값이다. */
    private static final int MAX_SAMPLE_IMAGES = 5;

    /** 메뉴 이름 길이 상한. menu_table.menu_name 이 VARCHAR(32) 라 거기 맞춘다. */
    private static final int MAX_MENU_NAME_LENGTH = 32;

    /**
     * 사장 회원가입이 확정될 때 가게를 만든다.
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

    /** 홈 목록. 최신 가게가 먼저 온다. 페이지네이션 지원 (20개씩 응답). */
    @Transactional(readOnly = true)
    public List<StoreSummaryResponse> findAll(int page, int size) {
        System.out.println("PAGE = " + page);
    System.out.println("SIZE = " + size);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return stores.findAllByOrderByIdDesc(pageable)
                .map(StoreService::toSummary)
                .getContent();
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
     * POST /api/stores/me/menus. 새 메뉴를 한 줄 만든다.
     *
     * 수정(updateMyMenu)과 달리 이름·가격을 받는다 — 그 둘은 생성 때만 정하고
     * 그 뒤로는 고치지 않는다.
     *
     * 표본 사진을 최소 한 장 요구하는 것도 updateMyMenu 와 같다 — 표본이 없으면
     * AI 대조 기준이 없어 그 메뉴로는 리뷰를 받을 수 없는 메뉴가 된다.
     */
    @Transactional
    public MenuOwnerResponse createMyMenu(long userId, String rawName, int price, String imageUrl,
            List<String> sampleImageUrls, boolean reviewEvent) {
        Store store = storeOf(requireOwner(userId));

        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new ValidationException("MENU_NAME_REQUIRED", "메뉴 이름을 입력해 주세요");
        }
        if (name.length() > MAX_MENU_NAME_LENGTH) {
            throw new ValidationException("MENU_NAME_TOO_LONG",
                    "메뉴 이름은 " + MAX_MENU_NAME_LENGTH + "자 이하여야 합니다");
        }
        if (price < 0) {
            throw new ValidationException("MENU_PRICE_INVALID", "가격은 0원 이상이어야 합니다");
        }
        // 같은 가게 안에서만 막는다. 다른 가게에 같은 이름의 메뉴가 있는 건 정상이다.
        if (menus.existsByStoreIdAndName(store.getId(), name)) {
            throw new ConflictException("MENU_NAME_TAKEN", "이미 쓰이고 있는 메뉴 이름입니다");
        }

        List<String> samples = sampleImageUrls == null ? List.of() : sampleImageUrls;
        requireValidSamples(samples);

        // 생성자는 표본 5칸을 모른다. 칸을 채우는 규칙을 두 군데 두지 않도록
        // 저장 전에 applyEdit 을 한 번 통과시킨다 — updateMyMenu 와 같은 코드를 타게 된다.
        Menu menu = new Menu(store, name, price, imageUrl, reviewEvent);
        menu.applyEdit(imageUrl, samples, reviewEvent);
        Menu saved = menus.saveAndFlush(menu);

        // 생성 시각·갱신 시각은 DB 기본값(CURRENT_TIMESTAMP)으로 채워진다.
        // INSERT 후 다시 읽어와야 응답에 그 두 값을 실어 보낼 수 있다.
        entityManager.refresh(saved);

        if (reviewEvent) {
            store.markReviewing(true);
        }

        return toMenuOwnerResponse(saved);
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
        requireValidSamples(samples);

        menu.applyEdit(imageUrl, samples, reviewEvent);
        store.markReviewing(menus.existsByStoreIdAndReviewEventTrue(store.getId()));

        return toMenuOwnerResponse(menu);
    }

    /**
     * 표본 사진 규칙. 메뉴 추가와 메뉴 수정이 같은 기준을 써야 해서 한 군데 모아 둔다.
     *
     * 빈 칸(null)은 허용하되 전부 비어 있으면 거절한다 — 한 장도 없으면 AI 대조를 못 한다.
     *
     * 갯수만 보는 게 아니라 그 주소가 가리키는 파일까지 열어본다. 업로드 때 크기를
     * 검사하지만(UploadController 의 minLongEdge) 그건 부르는 쪽이 선택하는 값이라,
     * 검사 없이 받은 주소를 그대로 표본 칸에 넣으면 아무도 다시 보지 않았다.
     * 붙이는 시점인 여기서 보면 주소를 어떻게 얻어 왔든 상관없어진다.
     */
    private void requireValidSamples(List<String> samples) {
        if (samples.size() > MAX_SAMPLE_IMAGES) {
            throw new ValidationException("TOO_MANY_SAMPLE_IMAGES",
                    "표본 사진은 " + MAX_SAMPLE_IMAGES + "장까지만 등록할 수 있습니다");
        }
        if (samples.stream().noneMatch(Objects::nonNull)) {
            throw new ValidationException("SAMPLE_IMAGE_REQUIRED", "표본 사진을 한 장 이상 등록해야 합니다");
        }

        // 리뷰 사진에 요구하는 하한과 같은 값을 쓴다 — 대조하는 두 사진 중 기준 쪽만
        // 저화질이면 같은 음식을 찍은 리뷰도 유사도가 낮게 나와 거부된다.
        int minLongEdge = properties.review().minImageLongEdge();

        for (String url : samples) {
            if (url == null) {
                continue;
            }
            // 파일이 없는 경우와 이미지로 못 읽는 경우를 하나로 묶는다 — 화면 입장에선
            // 둘 다 "이 사진은 못 쓴다"로 같고, 구분해 알려 주면 서버에 어떤 파일이
            // 있는지를 떠볼 수 있게 된다.
            if (!storage.exists(url)) {
                throw new ValidationException("SAMPLE_IMAGE_NOT_FOUND", "표본 사진을 찾을 수 없습니다");
            }

            int longEdge;
            try {
                longEdge = resizer.longEdge(storage.pathOf(url));
            } catch (IllegalArgumentException e) {
                throw new ValidationException("SAMPLE_IMAGE_NOT_FOUND", "표본 사진을 이미지로 읽을 수 없습니다");
            }
            if (longEdge < minLongEdge) {
                throw new ValidationException("SAMPLE_IMAGE_TOO_SMALL",
                        "표본 사진은 긴 변이 " + minLongEdge + "px 이상이어야 합니다");
            }
        }
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
