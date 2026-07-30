package com.reviewticket.server.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.repository.FoodRepository;
import com.reviewticket.server.repository.ReviewRepository;

import jakarta.persistence.EntityManager;

/**
 * MySQL JSON 컬럼에 Map<String,Double> 이 그대로 매핑되는지 확인한다.
 * 자료가 "Oracle/PostgreSQL 만 지원"이라고 엇갈려서 직접 돌려본다.
 *
 * @Transactional 이라 끝나면 롤백된다 — 실제 reviews 테이블에 남지 않는다.
 */
@SpringBootTest
// 실서비스 DB 를 비우지 않도록 reviewticket_test 로 붙는다
@ActiveProfiles({ "local", "test" })
@Transactional
class JsonMappingTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void aiProbsRoundTripsThroughJsonColumn() {
        Food hamburger = foodRepository.findByName("hamburger").orElseThrow();

        // FastAPI 가 실제로 돌려준 모양 그대로 (확률 6개, 합 1)
        Map<String, Double> probs = new LinkedHashMap<>();
        probs.put("bibimbap", 0.0014079575194045901);
        probs.put("chicken_wings", 0.0018717412604019046);
        probs.put("hamburger", 0.9741060733795166);
        probs.put("non_food", 4.589789023157209e-05);
        probs.put("pizza", 0.001067404751665890);
        probs.put("ramen", 0.021500898525118828);

        Review saved = reviewRepository.save(new Review(
                hamburger,
                4,
                "햄버거 패티가 두툼했어요. 감자튀김도 바삭.",
                "./Uploaded_pictures/json-mapping-test.jpg",
                "a".repeat(64),
                1234567890123L,
                "hamburger",
                new BigDecimal("0.000046"),
                probs));

        // 1차 캐시를 비워 DB 에서 실제로 다시 읽게 만든다
        entityManager.flush();
        entityManager.clear();

        Review loaded = reviewRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getAiProbs())
                .hasSize(6)
                .containsEntry("hamburger", 0.9741060733795166)
                .containsEntry("non_food", 4.589789023157209e-05);

        // 나머지 컬럼도 왕복 확인 — 한글, TINYINT, DECIMAL, BIGINT
        assertThat(loaded.getContent()).isEqualTo("햄버거 패티가 두툼했어요. 감자튀김도 바삭.");
        assertThat(loaded.getRating()).isEqualTo(4);
        assertThat(loaded.getImagePhash()).isEqualTo(1234567890123L);
        assertThat(loaded.getAiPNonFood()).isEqualByComparingTo("0.000046");
        assertThat(loaded.getExpectedFood().getName()).isEqualTo("hamburger");
        assertThat(loaded.getCreatedAt()).isNotNull();   // DB DEFAULT 가 채운다
    }
}
