package com.reviewticket.server.store;

import java.time.Instant;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reviewticket.server.domain.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 가게, 메뉴 조회와 사장의 가게 관리.
 */
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    public record UpdateStoreRequest(
            @NotBlank(message = "가게 이름을 입력해 주세요") String storeName,
            String logoUrl) {
    }

    /** 4.4 응답. 4.3(GET, 사장 본인 조회)보다 작다 — 방금 바뀐 값과 갱신 시각만 돌려주면 충분하다. */
    public record UpdateStoreResponse(Long storeId, String storeName, String logoUrl, Instant latestUpdate) {
    }

    /**
     * 메뉴 수정 요청. 세 값을 통째로 덮어쓴다 — 부분 수정은 없다.
     *
     * sampleImageUrls 는 최대 5개, 최소 1개는 값이 있어야 한다(비어 있으면
     * AI 대조 기준이 없어진다). 검증은 StoreService 가 한다.
     */
    public record UpdateMenuRequest(String imageUrl, @NotNull List<String> sampleImageUrls, boolean reviewEvent) {
    }

    /** 방금 바뀐 값과 갱신 시각만 돌려준다 — UpdateStoreResponse 와 같은 이유다. */
    public record UpdateMenuResponse(Long menuId, String menuImageUrl, List<String> sampleImageUrls,
            boolean reviewEvent, Instant menuLatestUpdate) {
    }

    /** 홈 목록. 개수 제한과 페이지네이션은 두지 않았다. */
    @GetMapping
    public List<StoreSummaryResponse> stores(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
         {
            return storeService.findAll(page, size);
        }
    }

    /** 주문 화면용 상세. 가게 정보 + 그 가게의 메뉴 배열. */
    @GetMapping("/{storeId}")
    public StoreDetailResponse store(@PathVariable Long storeId) {
        return storeService.findById(storeId);
    }

    /** 사장이 자기 가게를 조회한다. 가게 번호를 요청에 넣지 않는다 — 토큰의 주체가 곧 그 가게의 사장이다. */
    @GetMapping("/me")
    public StoreMeResponse myStore(@AuthenticationPrincipal User user) {
        return storeService.findMine(user.getId());
    }

    /** 가게 이름과 로고를 고친다. 두 값을 통째로 덮어쓰며, logoUrl 이 null 이면 로고를 지운다는 뜻이다. */
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UpdateStoreResponse updateMyStore(@AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateStoreRequest request) {
        StoreMeResponse updated = storeService.updateMine(user.getId(), request.storeName(), request.logoUrl());
        return new UpdateStoreResponse(updated.storeId(), updated.storeName(), updated.logoUrl(), updated.latestUpdate());
    }

    /** 사장이 자기 가게 메뉴를 조회한다. */
    @GetMapping("/me/menus")
    public List<MenuOwnerResponse> myMenus(@AuthenticationPrincipal User user) {
        return storeService.findMyMenus(user.getId());
    }

    /** 대표 사진·표본 사진(최대 5장)·리뷰이벤트 여부를 고친다. 메뉴 이름·가격은 여전히 고정이다. */
    @PatchMapping(value = "/me/menus/{menuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UpdateMenuResponse updateMyMenu(@AuthenticationPrincipal User user, @PathVariable Long menuId,
            @Valid @RequestBody UpdateMenuRequest request) {
        MenuOwnerResponse updated = storeService.updateMyMenu(user.getId(), menuId,
                request.imageUrl(), request.sampleImageUrls(), request.reviewEvent());
        return new UpdateMenuResponse(updated.menuId(), updated.menuImageUrl(), updated.sampleImageUrls(),
                updated.reviewEvent(), updated.menuLatestUpdate());
    }
}
