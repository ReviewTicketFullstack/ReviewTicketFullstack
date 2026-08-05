package com.reviewticket.server.order;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Order;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.MenuRepository;
import com.reviewticket.server.repository.OrderRepository;

/**
 * 주문 생성과 조회.
 *
 * 가격은 요청에서 받지 않는다 — 프론트가 보낸 값을 그대로 저장하면 개발자
 * 도구로 금액을 고쳐 보낼 수 있다. menuId 로 서버가 직접 조회해 담는다.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final MenuRepository menus;
    private final ReviewTicketProperties properties;

    public OrderService(OrderRepository orders, MenuRepository menus, ReviewTicketProperties properties) {
        this.orders = orders;
        this.menus = menus;
        this.properties = properties;
    }

    @Transactional
    public OrderResponse create(User user, Long storeId, Long menuId) {
        Menu menu = menus.findById(menuId)
                .orElseThrow(() -> new ValidationException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다"));

        // storeId 를 믿지 않고 메뉴가 실제로 그 가게 것인지 확인한다. 어긋난
        // 조합이 그대로 저장되면 주문 내역의 가게 이름이 엉뚱해진다.
        if (!menu.getStore().getId().equals(storeId)) {
            throw new ValidationException("MENU_STORE_MISMATCH", "메뉴가 해당 가게의 것이 아닙니다");
        }

        LocalDateTime deadline = LocalDateTime.now().plus(properties.order().reviewTtl());
        Order saved = orders.save(new Order(user, menu, deadline));
        return toResponse(saved);
    }

    /** 로그인한 본인 주문만. 최신 주문이 먼저 온다. */
    @Transactional(readOnly = true)
    public List<OrderResponse> findMine(User user) {
        return orders.findMyOrders(user).stream()
                .map(OrderService::toResponse)
                .toList();
    }

    private static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStore().getId(),
                order.getStore().getName(),
                order.getMenuName(),
                order.getPrice(),
                order.isReviewEvent(),
                order.isReviewable(LocalDateTime.now()) ? "available" : "not_available",
                toUtc(order.getReviewDeadline()),
                toUtc(order.getCreatedAt()));
    }

    /**
     * 저장된 값은 서버 시간대(LocalDateTime)라 그대로 내보내면 "2026-08-04T09:30:00"
     * 처럼 기준이 빠진 문자열이 된다. 프론트가 한국 시간으로 바꿔 표시하므로
     * 기준이 분명한 UTC(끝에 Z)로 바꿔서 보낸다.
     */
    private static Instant toUtc(LocalDateTime serverTime) {
        return serverTime == null ? null : serverTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
