package com.reviewticket.server.review;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.ForbiddenException;
import com.reviewticket.server.auth.NotFoundException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Order;
import com.reviewticket.server.domain.Review;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.image.ImageStorage;
import com.reviewticket.server.repository.OrderRepository;
import com.reviewticket.server.repository.ReviewRepository;
import com.reviewticket.server.repository.UserRepository;

/**
 * 리뷰 등록의 DB 트랜잭션 경계.
 *
 * ReviewService 에서 분리해 별도 빈으로 둔 이유가 둘이다.
 *
 * 하나는 트랜잭션을 짧게 유지하기 위해서다. 예전에는 리뷰 등록 전체가 하나의
 * @Transactional 이라, AI 서버 응답을 기다리는 3~10초 동안 DB 커넥션을 붙잡고
 * 있었다. 커넥션 풀이 기본 10개라 리뷰가 동시에 열 건만 들어와도 다른 모든
 * API 가 커넥션을 못 얻어 멈춘다. 지금은 AI 호출이 트랜잭션 바깥에 있다.
 *
 * 다른 하나는 프록시 때문이다. 같은 클래스 안에서 자기 메서드를 부르면 프록시를
 * 지나지 않아 @Transactional 이 통째로 무시된다(시도 로그에서 이미 겪었다).
 * 빈을 나눠야 호출이 반드시 프록시를 통과한다.
 */
@Component
public class ReviewTransaction {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final UserRepository users;
    private final ImageStorage storage;

    public ReviewTransaction(ReviewRepository reviews, OrderRepository orders, UserRepository users,
            ImageStorage storage) {
        this.reviews = reviews;
        this.orders = orders;
        this.users = users;
        this.storage = storage;
    }

    /**
     * 자격을 확인하고 AI 대조에 쓸 표본 사진 주소만 복사해 돌려준다.
     *
     * 엔티티를 그대로 넘기지 않는다 — 트랜잭션이 끝나면 준영속 상태가 되어
     * 지연 로딩이 터진다. 값만 꺼내 나간다.
     */
    @Transactional(readOnly = true)
    public List<String> prepare(long userId, Long orderId) {
        Order order = requireWritableOrder(userId, orderId);

        Menu menu = order.getMenu();
        List<String> sampleUrls = menu.getSampleImageUrls().stream().filter(Objects::nonNull).toList();
        if (sampleUrls.isEmpty()) {
            throw new ValidationException("MENU_SAMPLE_MISSING", "이 메뉴에 등록된 표본 사진이 없습니다");
        }
        return sampleUrls;
    }

    /**
     * 판정을 통과한 리뷰를 저장한다. 사진 기록, 리뷰 행 생성, 티켓 반환,
     * 가게 통계 갱신이 한 트랜잭션이다.
     *
     * 자격을 여기서 다시 본다. prepare 이후 AI 판정에 몇 초가 걸리는 동안
     * 마감이 지나거나 다른 요청이 먼저 리뷰를 달 수 있기 때문이다 — 앞의
     * 확인은 헛수고를 줄이기 위한 것이고, 최종 방어선은 이쪽이다.
     */
    @Transactional
    public ReviewCreateResponse commit(long userId, Long orderId, int rating, String content,
            byte[] reviewImage, double similarity, String compareImageUrl) {
        // 티켓을 되돌리는 트랜잭션이라 users 행을 맨 앞에서 배타 락으로 잡는다.
        // 아래 reviews.save() 가 외래키 때문에 같은 행에 공유 락을 먼저 걸어,
        // 그 상태에서 티켓을 UPDATE 하면 락 승격 데드락이 난다 — 주문 생성과
        // 똑같은 구조다(UserRepository.findByIdForUpdate 주석 참고).
        User customer = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "로그인이 필요합니다"));

        Order order = requireWritableOrder(userId, orderId);

        String reviewImageUrl = storage.save(reviewImage);
        Review saved = reviews.save(
                new Review(order, rating, content, reviewImageUrl, similarity, compareImageUrl));

        customer.refundTicket();
        order.getStore().recordReview(rating);

        return new ReviewCreateResponse(saved.getId(), order.getId(), saved.getStore().getId(),
                saved.getMenu().getId(), saved.getUser().getId(), saved.getRating(), saved.getContent(),
                saved.getImageUrl(), toUtc(saved.getCreatedAt()), saved.getImageSimilarity(),
                saved.getCompareImageUrl(), order.getCustomer().getTickets());
    }

    /** 본인 주문인지, 이벤트에 참여했는지, 기한이 남았는지, 이미 쓴 리뷰가 있는지. */
    private Order requireWritableOrder(long userId, Long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "그 번호의 주문이 없습니다"));
        if (!order.getCustomer().getId().equals(userId)) {
            throw new ForbiddenException("NOT_ORDER_OWNER", "남의 주문에 리뷰를 쓰려 했습니다");
        }
        if (!order.isReviewEventApply()) {
            throw new ValidationException("REVIEW_EVENT_NOT_APPLIED", "이벤트에 참여하지 않은 주문입니다");
        }
        if (order.isExpired(LocalDateTime.now())) {
            throw new ValidationException("REVIEW_PERIOD_EXPIRED", "마감 시각이 지났습니다");
        }
        if (reviews.existsByOrderId(orderId)) {
            throw new ConflictException("REVIEW_ALREADY_EXISTS", "그 주문에 이미 리뷰가 있습니다");
        }
        return order;
    }

    private static Instant toUtc(LocalDateTime serverTime) {
        return serverTime == null ? null : serverTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
