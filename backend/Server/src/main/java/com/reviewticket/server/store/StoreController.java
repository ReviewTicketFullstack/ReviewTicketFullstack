package com.reviewticket.server.store;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가게 조회.
 *
 * 두 라우터 모두 로그인이 필요하다 — SecurityConfig 가 /api/auth/** 를 뺀
 * 나머지를 인증 대상으로 두고 있고, 홈 화면 자체가 로그인 뒤에 있다.
 */
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /** 홈 목록. 최신 가게가 먼저 온다. 개수 제한과 페이지네이션은 두지 않았다. */
    @GetMapping
    public List<StoreSummaryResponse> stores() {
        return storeService.findAll();
    }

    /** 주문 화면용 상세. 가게 정보 + 그 가게의 메뉴 배열. */
    @GetMapping("/{storeId}")
    public StoreDetailResponse store(@PathVariable Long storeId) {
        return storeService.findById(storeId);
    }
}
