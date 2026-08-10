package com.reviewticket.server.review;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.ForbiddenException;
import com.reviewticket.server.auth.ImageNotMatchedException;
import com.reviewticket.server.auth.NotFoundException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.Menu;
import com.reviewticket.server.domain.Order;
import com.reviewticket.server.domain.Review;
import com.reviewticket.server.domain.Store;
import com.reviewticket.server.image.ImageResizer;
import com.reviewticket.server.image.ImageStorage;
import com.reviewticket.server.repository.OrderRepository;
import com.reviewticket.server.repository.ReviewRepository;
import com.reviewticket.server.store.StoreService;

/**
 * 리뷰 작성과 조회.
 *
 * 작성(create)의 핵심 원칙 — 판정을 통과하기 전까지 사진은 단 한 번도 디스크에
 * 닿지 않는다. 리사이즈까지 전부 메모리(byte[])에서 처리하고, AI 서버 유사도가
 * 문턱값을 넘겼을 때만 그제서야 ImageStorage 로 저장한다. 그래서 실패한 시도는
 * 지울 파일도 지우는 코드도 필요 없다 — 요청이 끝나면 메모리가 회수되면서
 * 같이 사라진다. 7장(업로드 API)을 리뷰 사진에 쓰지 않는 이유도 같다.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final StoreService storeService;
    private final ImageResizer resizer;
    private final ImageStorage storage;
    private final ImageSimilarityClient aiClient;
    private final ReviewTicketProperties properties;

    /**
     * 표본 사진 최대 5장을 동시에 대조하기 위한 전용 풀. 순차로 부르면 AI 서버
     * 응답이 실측 최대 3.5초라 최악의 경우 5배(17초 이상)로 늘어난다 — 유저가
     * 그만큼 로딩 화면을 더 본다. 동시에 쏘면 전체 소요 시간이 가장 느린 한
     * 번의 호출 수준으로 유지된다.
     */
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(5);

    public ReviewService(ReviewRepository reviews, OrderRepository orders, StoreService storeService,
            ImageResizer resizer, ImageStorage storage, ImageSimilarityClient aiClient,
            ReviewTicketProperties properties) {
        this.reviews = reviews;
        this.orders = orders;
        this.storeService = storeService;
        this.resizer = resizer;
        this.storage = storage;
        this.aiClient = aiClient;
        this.properties = properties;
    }

    @PreDestroy
    void shutdownAiExecutor() {
        aiExecutor.shutdown();
    }

    /** 표본 사진 한 장과의 대조 결과. AI 판정을 통과하면 url 이 그대로 compareImageUrl 로 남는다. */
    private record SampleMatch(String url, double similarity) {
    }

    @Transactional
    public ReviewCreateResponse create(long userId, Long orderId, int rating, String rawContent, MultipartFile image) {
        // ---- 입력값 형식 검증. 화면이 이미 걸러내지만(별점/후기/사진 세 가지가
        // 다 차야 제출 버튼이 켜진다) 요청은 화면을 거치지 않고도 올 수 있다.
        if (rating < 1 || rating > 5) {
            throw new ValidationException("INVALID_RATING", "별점이 1에서 5 밖입니다");
        }
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.length() < properties.review().contentMinLength()) {
            throw new ValidationException("CONTENT_TOO_SHORT", "후기가 너무 짧습니다");
        }
        if (content.length() > properties.review().contentMaxLength()) {
            throw new ValidationException("CONTENT_TOO_LONG", "후기가 너무 깁니다");
        }
        if (image == null || image.isEmpty()) {
            throw new ValidationException("IMAGE_REQUIRED", "사진이 없습니다");
        }
        if (!ImageResizer.SUPPORTED_TYPES.contains(image.getContentType())) {
            throw new ValidationException("UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 이미지 형식입니다");
        }

        // ---- 주문 자격 확인. 본인 주문인지, 이벤트에 참여했는지, 기한이
        // 남았는지, 이미 쓴 리뷰가 있는지 순서로 본다.
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

        // ---- 사진 준비. 아직 디스크에 닿지 않는다.
        byte[] rawBytes = readBytes(image);
        if (resizer.longEdge(rawBytes) < properties.review().minImageLongEdge()) {
            throw new ValidationException("IMAGE_TOO_SMALL", "긴 변이 " + properties.review().minImageLongEdge() + "px 보다 작습니다");
        }
        ImageResizer.Resized resized = resizer.resize(rawBytes, properties.upload().targetLongEdge());

        Menu menu = order.getMenu();
        List<String> sampleUrls = menu.getSampleImageUrls().stream().filter(Objects::nonNull).toList();
        if (sampleUrls.isEmpty()) {
            throw new ValidationException("MENU_SAMPLE_MISSING", "이 메뉴에 등록된 표본 사진이 없습니다");
        }

        // ---- AI 판정. 여기서 처음으로 외부 서비스를 부른다. 표본 사진 전부를
        // 동시에 대조해 유사도가 가장 높은 것을 쓴다 — 다섯 칸은 동등하므로
        // 어느 각도로 찍었든 하나만 맞으면 통과할 수 있어야 한다.
        List<CompletableFuture<SampleMatch>> futures = sampleUrls.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> {
                    byte[] compareBytes = storage.read(url);
                    return new SampleMatch(url, aiClient.measureSimilarity(resized.bytes(), compareBytes));
                }, aiExecutor))
                .toList();

        SampleMatch best;
        try {
            best = futures.stream()
                    .map(CompletableFuture::join)
                    .max(Comparator.comparingDouble(SampleMatch::similarity))
                    .orElseThrow();
        } catch (CompletionException e) {
            // futures 안에서 던진 예외(AI_SERVER_UNAVAILABLE 등)는 CompletionException 으로
            // 감싸여 나온다. ApiExceptionHandler 가 원래 예외를 알아보게 벗겨서 다시 던진다.
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }

        if (best.similarity() < properties.ai().matchThreshold()) {
            throw new ImageNotMatchedException(best.similarity());
        }

        // ---- 통과. 이제야 사진을 저장하고, 리뷰 행 생성·티켓 반환·가게 통계
        // 갱신을 한 트랜잭션으로 처리한다. compareImageUrl 은 대표 사진이 아니라
        // 방금 가장 높은 유사도를 낸 그 표본 사진이다.
        String reviewImageUrl = storage.save(resized.bytes());
        Review saved = reviews.save(new Review(order, rating, content, reviewImageUrl, best.similarity(), best.url()));

        order.getCustomer().refundTicket();
        order.getStore().recordReview(rating);

        return new ReviewCreateResponse(saved.getId(), order.getId(), saved.getStore().getId(), saved.getMenu().getId(),
                saved.getUser().getId(), saved.getRating(), saved.getContent(), saved.getImageUrl(),
                toUtc(saved.getCreatedAt()), saved.getImageSimilarity(), saved.getCompareImageUrl(),
                order.getCustomer().getTickets());
    }


    /** 그 가게에 달린 리뷰 전부, 최신순. 누가 썼는지 가리지 않는다. */
    @Transactional(readOnly = true)
    public List<ReviewPublicResponse> findByStore(Long storeId) {
        Store store = storeService.loadStore(storeId);
        return reviews.findByStoreOrderByCreatedAtDesc(store).stream()
                .map(r -> new ReviewPublicResponse(r.getId(), r.getMenu().getId(), r.getMenu().getName(),
                        r.getUser().getDisplayName(), r.getRating(), r.getContent(), r.getImageUrl(),
                        toUtc(r.getCreatedAt())))
                .toList();
    }

    /** 사장의 리뷰관리 화면, 리뷰완료 탭. */
    @Transactional(readOnly = true)
    public ReviewOwnerListResponse findMineOwner(long userId) {
        Store store = storeService.requireOwnerStore(userId);
        List<ReviewOwnerItemResponse> items = reviews.findByStoreOrderByCreatedAtDesc(store).stream()
                .map(r -> new ReviewOwnerItemResponse(r.getId(), r.getOrder().getId(), r.getMenu().getId(),
                        r.getMenu().getName(), r.getOrder().getPrice(), r.getUser().getDisplayName(),
                        r.getRating(), r.getContent(), r.getImageUrl(), toUtc(r.getCreatedAt()),
                        r.getImageSimilarity(), r.getCompareImageUrl()))
                .toList();
        return new ReviewOwnerListResponse(store.getReviewNumber(), store.getReviewValue(), items);
    }

    /** 사장의 리뷰관리 화면, 리뷰미작성 탭. 이벤트에 참여했으나 아직 리뷰가 없는 주문이다. */
    @Transactional(readOnly = true)
    public List<PendingOrderResponse> findPendingByOwner(long userId) {
        Store store = storeService.requireOwnerStore(userId);
        LocalDateTime now = LocalDateTime.now();

        return orders.findPendingReviewOrders(store).stream()
                .map(o -> new PendingOrderResponse(o.getId(), o.getCustomer().getDisplayName(), o.getMenu().getId(),
                        o.getMenuName(), o.getPrice(), toUtc(o.getOrderedAt()), toUtc(o.getExpireTime()),
                        o.isExpired(now) ? "expired" : "pending"))
                .toList();
    }

    private static byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new ValidationException("IMAGE_REQUIRED", "사진을 읽을 수 없습니다");
        }
    }

    private static Instant toUtc(LocalDateTime serverTime) {
        return serverTime == null ? null : serverTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
