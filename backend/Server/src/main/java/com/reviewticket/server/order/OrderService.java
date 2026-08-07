package com.reviewticket.server.order;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ForbiddenException;
import com.reviewticket.server.auth.NotFoundException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Order;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.MenuRepository;
import com.reviewticket.server.repository.OrderRepository;
import com.reviewticket.server.repository.ReviewRepository;
import com.reviewticket.server.repository.UserRepository;

/**
 * 주문 생성과 조회.
 *
 * 가격은 요청에서 받지 않는다 — 프론트가 보낸 값을 그대로 저장하면 개발자
 * 도구로 금액을 고쳐 보낼 수 있다. menuId 로 서버가 직접 조회해 담는다.
 *
 * 티켓은 주문이 만들어질 때(reviewEventApply=true) 미리 잠그고, 리뷰가
 * 검증을 통과해 저장될 때(ReviewService) 돌려준다. 기한이 지나도록 리뷰가
 * 없으면 그냥 소멸한다 — 8장(설계서) 참고. 이 방식 덕분에 "동시에 진행할 수
 * 있는 이벤트 주문은 티켓 개수만큼"이라는 정책이 별도 카운팅 없이 지켜진다.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final MenuRepository menus;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final ReviewTicketProperties properties;

    public OrderService(OrderRepository orders, MenuRepository menus, ReviewRepository reviews,
            UserRepository users, ReviewTicketProperties properties) {
        this.orders = orders;
        this.menus = menus;
        this.reviews = reviews;
        this.users = users;
        this.properties = properties;
    }

    @Transactional
    public OrderCreateResponse create(long userId, Long storeId, Long menuId, boolean reviewEventApply) {
        User customer = requireCustomer(userId);

        Menu menu = menus.findById(menuId)
                .orElseThrow(() -> new NotFoundException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다"));

        // storeId 를 믿지 않고 메뉴가 실제로 그 가게 것인지 확인한다. 어긋난
        // 조합이 그대로 저장되면 주문 내역의 가게 이름이 엉뚱해진다.
        if (!menu.getStore().getId().equals(storeId)) {
            throw new ValidationException("MENU_STORE_MISMATCH", "메뉴가 해당 가게의 것이 아닙니다");
        }
        if (reviewEventApply && !menu.isReviewEvent()) {
            throw new ValidationException("REVIEW_EVENT_NOT_AVAILABLE", "이벤트 대상이 아닌 메뉴에 참여를 신청했습니다");
        }

        Integer reviewDeadlineSeconds = null;
        LocalDateTime expireTime = null;
        if (reviewEventApply) {
            if (customer.getTickets() <= 0) {
                throw new ValidationException("NO_TICKETS_LEFT", "잠글 티켓이 없습니다");
            }
            customer.lockTicket();
            reviewDeadlineSeconds = (int) properties.order().reviewTtl().toSeconds();
            expireTime = LocalDateTime.now().plusSeconds(reviewDeadlineSeconds);
        }

        Order saved = orders.save(new Order(customer, menu, reviewEventApply, reviewDeadlineSeconds, expireTime));
        String reviewStatus = reviewEventApply ? "available" : "not_available";

        return new OrderCreateResponse(saved.getId(), saved.getStore().getId(), saved.getStore().getName(),
                saved.getMenu().getId(), saved.getMenuName(), saved.getPrice(), saved.isReviewEventApply(),
                toUtc(saved.getExpireTime()), toUtc(saved.getOrderedAt()),
                reviewStatus, customer.getTickets());
    }

    /** 로그인한 본인 주문만. 최신 주문이 먼저 온다. */
    @Transactional(readOnly = true)
    public List<OrderResponse> findMine(long userId) {
        User customer = requireCustomer(userId);
        List<Order> mine = orders.findMyOrders(customer);
        Set<Long> reviewed = reviewedOrderIds(mine);
        LocalDateTime now = LocalDateTime.now();

        return mine.stream()
                .map(order -> toResponse(order, reviewStatus(order, reviewed, now)))
                .toList();
    }

    private Set<Long> reviewedOrderIds(List<Order> mine) {
        List<Long> ids = mine.stream().map(Order::getId).toList();
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(reviews.findOrderIdsWithReview(ids));
    }

    /**
     * not_available: 애초에 이벤트에 참여하지 않은 주문.
     * done: 리뷰가 이미 있음.
     * expired: 마감이 지났고 리뷰도 없음.
     * available: 그 밖의 경우, 지금 리뷰를 쓸 수 있다.
     *
     * "검토 대기" 상태는 두지 않는다 — AI 검증이 제출과 동시에 끝나 승인을
     * 기다리는 구간 자체가 없다.
     */
    private static String reviewStatus(Order order, Set<Long> reviewedOrderIds, LocalDateTime now) {
        if (!order.isReviewEventApply()) {
            return "not_available";
        }
        if (reviewedOrderIds.contains(order.getId())) {
            return "done";
        }
        return order.isExpired(now) ? "expired" : "available";
    }

    /** 사장이 주문을 시도하면 막는다. 고객 전용 기능이다. */
    private User requireCustomer(long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "로그인이 필요합니다"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("NOT_CUSTOMER", "사장 계정으로 주문을 시도했습니다");
        }
        return user;
    }

    private static OrderResponse toResponse(Order order, String reviewStatus) {
        return new OrderResponse(order.getId(), order.getStore().getId(), order.getStore().getName(),
                order.getMenu().getId(), order.getMenuName(), order.getPrice(), order.isReviewEventApply(),
                toUtc(order.getExpireTime()), toUtc(order.getOrderedAt()), reviewStatus);
    }

    /**
     * 저장된 값은 서버 시간대(LocalDateTime)라 그대로 내보내면 기준이 빠진
     * 문자열이 된다. 프론트가 알아서 표시 시간대로 바꾸도록 기준이 분명한
     * UTC(끝에 Z)로 바꿔서 보낸다.
     */
    private static Instant toUtc(LocalDateTime serverTime) {
        return serverTime == null ? null : serverTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
