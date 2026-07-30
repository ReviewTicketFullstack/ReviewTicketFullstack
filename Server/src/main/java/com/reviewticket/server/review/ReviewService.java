package com.reviewticket.server.review;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.reviewticket.server.ai.AiClient;
import com.reviewticket.server.ai.Decision;
import com.reviewticket.server.ai.DecisionRule;
import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.Food;
import com.reviewticket.server.domain.RejectionReason;
import com.reviewticket.server.image.ImageHasher;
import com.reviewticket.server.image.ImageProcessor;
import com.reviewticket.server.repository.FoodRepository;
import com.reviewticket.server.repository.ReviewRepository;

/**
 * 리뷰 제출 흐름 전체.
 *
 * 검사 순서는 싼 것부터다 — 크기(Spring 이 자동) -> 디코딩 -> 해시 중복 ->
 * AI 호출(가장 비쌈). 해시로 걸러낼 수 있는 건 AI 를 부르기 전에 끝낸다.
 *
 * 이 클래스에는 @Transactional 이 없다. AI 호출을 트랜잭션 안에 넣으면 외부
 * HTTP 를 기다리는 동안 DB 커넥션을 붙잡기 때문이다. 트랜잭션은 판정이 끝난
 * 뒤 {@link ReviewWriter} 안에서만 열린다.
 */
@Service
public class ReviewService {

    private final ImageProcessor imageProcessor;
    private final ImageHasher imageHasher;
    private final AiClient aiClient;
    private final DecisionRule decisionRule;
    private final ReviewRepository reviewRepository;
    private final FoodRepository foodRepository;
    private final ReviewWriter reviewWriter;
    private final int phashThreshold;

    public ReviewService(ImageProcessor imageProcessor, ImageHasher imageHasher,
            AiClient aiClient, DecisionRule decisionRule, ReviewRepository reviewRepository,
            FoodRepository foodRepository, ReviewWriter reviewWriter,
            ReviewTicketProperties properties) {
        this.imageProcessor = imageProcessor;
        this.imageHasher = imageHasher;
        this.aiClient = aiClient;
        this.decisionRule = decisionRule;
        this.reviewRepository = reviewRepository;
        this.foodRepository = foodRepository;
        this.reviewWriter = reviewWriter;
        this.phashThreshold = properties.duplicate().phashThreshold();
    }

    public ReviewSubmitResponse submit(String foodName, int rating, String content, byte[] originalImage) {
        Food food = foodRepository.findByName(foodName)
                .orElseThrow(() -> new IllegalArgumentException("없는 메뉴: " + foodName));

        // 1. 축소 — 이후 모든 단계가 이 축소본 하나로 진행된다. 원본은 여기서 버린다.
        ImageProcessor.Processed processed;
        try {
            processed = imageProcessor.process(originalImage);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 처리할 수 없다", e);
        }

        // 2. 해시 — AI 보다 훨씬 싸므로 먼저 본다
        String sha256 = imageHasher.sha256(processed.jpegBytes());
        long phash = imageHasher.pHash(processed.image());

        if (isDuplicate(sha256, phash)) {
            // 중복은 AI 를 부르지 않는다. 그래서 로그의 AI 칸이 비어 있다.
            reviewWriter.saveRejection(food, RejectionReason.duplicate, null, null, null, sha256, phash);
            return ReviewSubmitResponse.from(
                    new Decision(false, RejectionReason.duplicate, null, 0.0), null, null);
        }

        // 3. AI 호출 — 트랜잭션 밖. 닿지 않으면 AiUnavailableException 이 올라가
        //    503 으로 나간다. 거부로 처리하지 않는다 — 우리 서버 잘못으로
        //    정상 사용자가 티켓을 잃으면 안 된다.
        Map<String, Double> probs = aiClient.predict(processed.jpegBytes(), "review.jpg");

        // 4. 판정 — 주문한 메뉴를 아는 건 여기뿐이다
        Decision decision = decisionRule.decide(probs, food.getName());
        BigDecimal pNonFood = toDecimal(decision.pNonFood());

        if (!decision.approved()) {
            reviewWriter.saveRejection(food, decision.reason(), decision.predicted(),
                    pNonFood, probs, sha256, phash);
            return ReviewSubmitResponse.from(decision, null, probs);
        }

        Long reviewId = reviewWriter.saveApproved(food, rating, content, processed.jpegBytes(),
                sha256, phash, decision, probs, pNonFood);
        return ReviewSubmitResponse.from(decision, reviewId, probs);
    }

    private boolean isDuplicate(String sha256, long phash) {
        if (reviewRepository.findByImageSha256(sha256).isPresent()) {
            return true;
        }
        // pHash 는 해밍 거리 비교라 인덱스를 못 쓴다. 값만 전부 읽어 메모리에서 본다.
        List<Long> known = reviewRepository.findAllPhashes();
        for (Long other : known) {
            if (ImageHasher.hammingDistance(phash, other) <= phashThreshold) {
                return true;
            }
        }
        return false;
    }

    /** DECIMAL(7,6) 에 맞춘다. 확률이라 항상 0~1 이다. */
    private static BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }
}
