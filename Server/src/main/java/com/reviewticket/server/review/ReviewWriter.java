package com.reviewticket.server.review;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.ai.Decision;
import com.reviewticket.server.domain.AiRejection;
import com.reviewticket.server.domain.Food;
import com.reviewticket.server.domain.RejectionReason;
import com.reviewticket.server.domain.Review;
import com.reviewticket.server.image.ImageStorage;
import com.reviewticket.server.repository.AiRejectionRepository;
import com.reviewticket.server.repository.ReviewRepository;

/**
 * DB 쓰기 담당. {@link ReviewService} 와 분리한 이유는 두 가지다.
 *
 * 1. @Transactional 은 프록시로 동작해서, 같은 클래스 안에서 자기 메서드를
 *    호출하면 적용되지 않는다. 별도 빈이어야 실제로 트랜잭션이 열린다.
 * 2. "AI 호출은 트랜잭션 밖" 이라는 규칙이 클래스 경계로 드러난다.
 */
@Component
public class ReviewWriter {

    private final ReviewRepository reviewRepository;
    private final AiRejectionRepository aiRejectionRepository;
    private final ImageStorage imageStorage;
    private final FailureLogWriter failureLogWriter;

    public ReviewWriter(ReviewRepository reviewRepository,
            AiRejectionRepository aiRejectionRepository, ImageStorage imageStorage,
            FailureLogWriter failureLogWriter) {
        this.reviewRepository = reviewRepository;
        this.aiRejectionRepository = aiRejectionRepository;
        this.imageStorage = imageStorage;
        this.failureLogWriter = failureLogWriter;
    }

    /**
     * 승인된 건만 온다. 사진 파일 쓰기와 리뷰 INSERT 를 한 트랜잭션으로 묶는다.
     *
     * 파일 쓰기는 트랜잭션 대상이 아니므로 DB 가 실패하면 직접 지운다.
     * 커밋 직후 프로세스가 죽는 경우에만 고아 파일이 남는데, 사진 한 장이
     * 놀고 있는 것이라 서비스에는 영향이 없다.
     */
    @Transactional
    public Long saveApproved(Food food, int rating, String content, byte[] jpegBytes,
            String sha256, long phash, Decision decision, Map<String, Double> probs,
            BigDecimal pNonFood) {

        String fileName = imageStorage.newFileName();
        try {
            imageStorage.write(fileName, jpegBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 저장할 수 없다", e);
        }

        try {
            Review review = new Review(food, rating, content,
                    imageStorage.resolve(fileName).toString(),
                    sha256, phash,
                    decision.predicted(), pNonFood, probs);
            return reviewRepository.save(review).getId();
        } catch (RuntimeException e) {
            imageStorage.deleteQuietly(fileName);
            throw e;
        }
    }

    /** 거부 로그. 사진 파일은 쓰지 않는다 — 메모리에서 그냥 버려진다. */
    @Transactional
    public void saveRejection(Food food, RejectionReason reason, String predicted,
            BigDecimal pNonFood, Map<String, Double> probs, String sha256, long phash) {
        aiRejectionRepository.save(
                new AiRejection(food, reason, predicted, pNonFood, probs, sha256, phash));

        // 사람이 읽을 텍스트 파일로도 남긴다. 실패해도 예외를 올리지 않으므로
        // 트랜잭션을 되돌리지 않는다.
        failureLogWriter.append(food, reason, predicted,
                pNonFood == null ? null : pNonFood.doubleValue(), probs, sha256, phash);
    }
}
