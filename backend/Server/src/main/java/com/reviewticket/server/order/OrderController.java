package com.reviewticket.server.order;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reviewticket.server.domain.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 주문. 대상 회원을 요청에서 받지 않는다 — 토큰의 주체가 곧 주문자다.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 가격은 받지 않는다. 서버가 menuId 로 조회해 담는다 —
     * 프론트가 보낸 금액을 그대로 믿으면 고쳐 보낼 수 있다.
     */
    public record CreateOrderRequest(
            @NotNull(message = "가게를 선택해 주세요") Long storeId,
            @NotNull(message = "메뉴를 선택해 주세요") Long menuId,
            boolean reviewEventApply) {
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderCreateResponse create(@AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(user.getId(), request.storeId(), request.menuId(), request.reviewEventApply());
    }

    /** 로그인한 본인 주문만, 최신순. */
    @GetMapping
    public List<OrderResponse> myOrders(@AuthenticationPrincipal User user) {
        return orderService.findMine(user.getId());
    }
}
