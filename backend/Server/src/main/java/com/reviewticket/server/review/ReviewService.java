package com.reviewticket.server.review;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.ImageVerifyException;
import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.server.auth.ImageNotMatchedException;
import com.reviewticket.server.auth.ServiceUnavailableException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.config.ReviewTicketProperties;
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
 *
 * create 에는 @Transactional 이 없다. DB 를 건드리는 두 구간(자격 확인, 저장)만
 * ReviewTransaction 이 각각 트랜잭션으로 감싸고, 그 사이의 이미지 처리와 AI
 * 호출은 트랜잭션 바깥에서 돈다 — 몇 초씩 걸리는 외부 호출을 트랜잭션 안에
 * 두면 그동안 DB 커넥션이 묶여 다른 요청까지 막힌다.
 *
 * 사진 대조 자체는 이제 이 클래스에 없다. 동시 호출, 최댓값 선택, 문턱값 비교는
 * 전부 이미지 검증 SDK(ImageVerifier) 안에 있다 — 그건 리뷰 업무 규칙이 아니라
 * 어느 서비스에서나 같은 방식으로 도는 일반적인 절차다. 여기 남은 것은 "무엇을
 * 대조할 것인가"와 "떨어졌을 때 무엇을 할 것인가"뿐이다.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final StoreService storeService;
    private final ReviewTransaction transaction;
    private final ImageResizer resizer;
    private final ImageStorage storage;
    private final ImageVerifier imageVerifier;
    private final ReviewTicketProperties properties;

    public ReviewService(ReviewRepository reviews, OrderRepository orders, StoreService storeService,
            ReviewTransaction transaction, ImageResizer resizer, ImageStorage storage,
            ImageVerifier imageVerifier, ReviewTicketProperties properties) {
        this.reviews = reviews;
        this.orders = orders;
        this.storeService = storeService;
        this.transaction = transaction;
        this.resizer = resizer;
        this.storage = storage;
        this.imageVerifier = imageVerifier;
        this.properties = properties;
    }

    public ReviewCreateResponse create(long userId, Long orderId, int rating, String rawContent, MultipartFile image) {
        // ---- 입력값 형식 검증. 화면이 이미 걸러내지만(별점/후기/사진 세 가지가
        // 다 차야 제출 버튼이 켜진다) 요청은 화면을 거치지 않고도 올 수 있다.
        // DB 를 보지 않으므로 트랜잭션 밖에서 먼저 끝낸다.
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

        // ---- [읽기 트랜잭션] 주문 자격을 확인하고 표본 사진 주소만 받아 나온다.
        List<String> sampleUrls = transaction.prepare(userId, orderId);

        // ---- 사진 준비. 아직 디스크에 닿지 않는다.
        byte[] rawBytes = readBytes(image);
        if (resizer.longEdge(rawBytes) < properties.review().minImageLongEdge()) {
            throw new ValidationException("IMAGE_TOO_SMALL", "긴 변이 " + properties.review().minImageLongEdge() + "px 보다 작습니다");
        }
        ImageResizer.Resized resized = resizer.resize(rawBytes, properties.upload().targetLongEdge());

        // ---- AI 판정. 트랜잭션 바깥이라 이 몇 초 동안 DB 커넥션을 붙잡지 않는다.
        VerificationResult verdict = verify(sampleUrls, resized.bytes());
        if (!verdict.matched()) {
            throw new ImageNotMatchedException(verdict.similarity());
        }

        // ---- [쓰기 트랜잭션] 자격을 다시 확인하고 저장한다. compareImageUrl 은
        // 대표 사진이 아니라 방금 가장 높은 유사도를 낸 그 표본 사진이다.
        return transaction.commit(userId, orderId, rating, content,
                resized.bytes(), verdict.similarity(), verdict.matchedKey());
    }

    /**
     * 표본 사진 전부와 대조해 가장 높은 유사도를 받는다.
     *
     * 다섯 칸은 동등하므로 어느 각도로 찍었든 하나만 맞으면 통과할 수 있어야
     * 한다. 동시 호출과 최댓값 선택은 SDK 안에서 일어난다.
     *
     * 표본 사진의 바이트를 미리 읽지 않고 읽는 방법만 넘긴다. SDK 는 그 사진이
     * 디스크에 있는지 어디에 있는지 알지 못하고, 알 필요도 없다. 표본 사진의
     * URL 을 그대로 key 로 쓰는데, ImageStorage 가 저장할 때마다 새 UUID 이름을
     * 짓기 때문에 URL 이 곧 내용을 가리키는 안정적인 이름이 된다.
     *
     * SDK 예외는 여기서 애플리케이션 예외로 번역한다 — 이 지점이 SDK 와
     * ReviewTicket 이 만나는 두 곳 중 하나다(다른 하나는 위의 matched 검사).
     * 예전에 ImageSimilarityClient 가 직접 던지던 것과 같은 코드로 나가므로
     * 화면이 받는 503 응답은 달라지지 않는다.
     */
    private VerificationResult verify(List<String> sampleUrls, byte[] reviewImage) {
        List<ReferenceImage> samples = sampleUrls.stream()
                .map(url -> ReferenceImage.of(url, () -> storage.read(url)))
                .toList();

        try {
            return imageVerifier.verify(samples, reviewImage);
        } catch (ImageVerifyException e) {
            throw new ServiceUnavailableException("AI_SERVER_UNAVAILABLE",
                    "AI 서버가 응답하지 않거나 시간을 초과했습니다", e);
        }
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
