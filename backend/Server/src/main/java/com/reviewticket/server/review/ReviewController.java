package com.reviewticket.server.review;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.reviewticket.server.domain.User;

/**
 * 리뷰 작성과 조회. 클래스 단위로 경로를 묶지 않는다 — /api/reviews 하나와
 * /api/stores/... 아래 세 개가 섞여 있어 공통 접두어가 없다.
 */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 리뷰 제출. multipart/form-data 로 받는다 — 사진이 함께 오므로 JSON 이 아니다.
     * storeId, menuId, userId 는 받지 않는다. orderId 로 주문 행을 찾으면 셋 다
     * 따라 나온다.
     */
    @PostMapping(value = "/api/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReviewCreateResponse create(@AuthenticationPrincipal User user,
            @RequestParam Long orderId,
            @RequestParam int reviewRating,
            @RequestParam String reviewContent,
            @RequestParam("image") MultipartFile image) {
        return reviewService.create(user.getId(), orderId, reviewRating, reviewContent, image);
    }

    /** 그 가게에 달린 리뷰를 전부 보여준다. 누가 썼는지는 가리지 않는다. */
    @GetMapping("/api/stores/{storeId}/reviews")
    public List<ReviewPublicResponse> storeReviews(@PathVariable Long storeId) {
        return reviewService.findByStore(storeId);
    }

    /** 사장의 리뷰관리 화면, 리뷰완료 탭. */
    @GetMapping("/api/stores/me/reviews")
    public ReviewOwnerListResponse myReviews(@AuthenticationPrincipal User user) {
        return reviewService.findMineOwner(user.getId());
    }

    /** 사장의 리뷰관리 화면, 리뷰미작성 탭. */
    @GetMapping("/api/stores/me/orders/pending")
    public List<PendingOrderResponse> pendingOrders(@AuthenticationPrincipal User user) {
        return reviewService.findPendingByOwner(user.getId());
    }
}
