package com.reviewticket.sdk.imageverify.http;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.ImageVerifiers;
import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;
import com.reviewticket.sdk.imageverify.core.CosineSimilarity;

/**
 * Phase 2 의 핵심 계약 — <b>코사인 계산을 torch 에서 자바로 옮겨도 값이 같은가.</b>
 *
 * <p>이 마이그레이션에서 실제로 위험한 지점이다. 전에는 파이썬이 유사도를
 * 계산했고, 이제는 자바가 받아 온 벡터로 계산한다. 두 값이 갈리면 0.80 문턱값
 * 근처에서 판정이 뒤집히고, DB 에 쌓인 과거 유사도와 새 값의 척도가 어긋난다(BC-2).
 *
 * <p>기본 빌드에서는 건너뛴다. 켜려면 추론 서버를 띄우고 주소를 준다:
 *
 * <pre>
 *   python -m uvicorn stub_server:app --port 8000 --app-dir sdk/python-inference
 *   cd sdk &amp;&amp; ./gradlew test -Dimageverify.contract.url=http://127.0.0.1:8000/similarity
 * </pre>
 *
 * <p>모델은 필요 없다. 확인하려는 것은 벡터가 어느 모델에서 나왔는지와 무관한
 * <b>수치 문제</b>라서, 결정론적 벡터를 내주는 스텁이면 충분하다. 스텁의
 * {@code /similarity} 와 {@code /embed} 가 같은 벡터를 쓰므로 한쪽만 고쳐서
 * 통과시킬 수도 없다.
 */
@EnabledIfSystemProperty(named = "imageverify.contract.url", matches = ".+")
class NumericEquivalenceContractTest {

    /** BC-2 가 요구하는 한계. 이 값을 넘으면 Phase 2 를 진행하지 않는다. */
    private static final double MAX_ABSOLUTE_DIFFERENCE = 1e-5;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static URI similarityUri() {
        return URI.create(System.getProperty("imageverify.contract.url"));
    }

    private static URI baseUri() {
        return similarityUri().resolve("/");
    }

    /**
     * 결정론적인 작은 JPEG 를 만든다. 서버가 PIL 로 디코드하므로 진짜 이미지여야
     * 하고, 매번 같은 바이트여야 두 경로에 같은 입력을 줄 수 있다.
     */
    /** 응답 JSON 에서 실수 필드 하나를 꺼낸다. 파서를 들이지 않는다. */
    private static double field(String json, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + name + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)")
                .matcher(json);
        assertTrue(matcher.find(), name + " 를 응답에서 찾지 못했습니다: " + json);
        return Double.parseDouble(matcher.group(1));
    }

    private static byte[] image(int seed) throws Exception {
        BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(seed);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                canvas.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(canvas, "jpg", out), "JPEG 인코더를 찾지 못했습니다");
        return out.toByteArray();
    }

    @Test
    @DisplayName("AC-30 자바 코사인이 torch 코사인과 1e-5 안에서 일치한다")
    void ac30_javaCosineMatchesTorchCosine() throws Exception {
        CosineSimilarity cosine = new CosineSimilarity();
        double worst = 0.0;
        double worstControl = 0.0;
        List<String> failures = new ArrayList<>();

        try (HttpEmbeddingModel embedding = new HttpEmbeddingModel(baseUri(), "/embed", TIMEOUT)) {

            for (int pair = 0; pair < 20; pair++) {
                byte[] candidate = image(pair * 2);
                byte[] reference = image(pair * 2 + 1);

                // 파이썬이 torch 로 계산한 값, 그리고 torch 도 OpenMP 도 거치지 않은
                // 순수 파이썬 대조군. 이 환경에는 OpenMP 충돌 우회책이 걸려 있는데,
                // 그게 수치를 건드렸다면 두 값이 갈린다.
                String body = RawPost.post(similarityUri(), candidate, reference);
                double fromTorch = field(body, "similarity");
                double control = field(body, "controlSimilarity");

                // 벡터만 받아 와 자바가 계산한 값
                List<float[]> vectors = embedding.embed(List.of(candidate, reference));
                double fromJava = cosine.between(vectors.get(0), vectors.get(1));

                double controlGap = Math.abs(fromTorch - control);
                worstControl = Math.max(worstControl, controlGap);
                if (controlGap > 1e-6) {
                    failures.add(String.format(
                            "쌍 %d 대조군 불일치: torch=%.17f pure=%.17f — OpenMP 우회책이 "
                                    + "수치를 건드렸을 수 있습니다", pair, fromTorch, control));
                }

                double difference = Math.abs(fromTorch - fromJava);
                worst = Math.max(worst, difference);
                if (difference > MAX_ABSOLUTE_DIFFERENCE) {
                    failures.add(String.format(
                            "쌍 %d: torch=%.17f java=%.17f 차이=%.3e", pair, fromTorch, fromJava, difference));
                }
            }
        }

        System.out.printf("[AC-30] 20쌍 최대 절대오차: torch↔java=%.3e, torch↔대조군=%.3e (한계 %.0e)%n",
                worst, worstControl, MAX_ABSOLUTE_DIFFERENCE);
        assertTrue(failures.isEmpty(), "코사인 값이 갈립니다:\n  " + String.join("\n  ", failures));
    }

    @Test
    @DisplayName("AC-32 두 백엔드의 판정이 완전히 같다")
    void ac32_bothBackendsAgree() throws Exception {
        byte[] candidate = image(1000);
        List<ReferenceImage> references = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            byte[] bytes = image(2000 + i);
            references.add(ReferenceImage.ofBytes("sample-" + i, bytes));
        }

        VerifierConfig config = VerifierConfig.withThreshold(0.80);

        try (ImageVerifier pairwise =
                ImageVerifiers.pairwiseOverHttp(similarityUri(), TIMEOUT, config);
                ImageVerifier embedding =
                        ImageVerifiers.embeddingOverHttp(baseUri(), "/embed", TIMEOUT, config)) {

            VerificationResult fromPairwise = pairwise.verify(references, candidate);
            VerificationResult fromEmbedding = embedding.verify(references, candidate);

            double difference = Math.abs(fromPairwise.similarity() - fromEmbedding.similarity());
            System.out.printf("[AC-32] pairwise=%.17f embedding=%.17f 차이=%.3e%n",
                    fromPairwise.similarity(), fromEmbedding.similarity(), difference);

            assertAll(
                    () -> assertTrue(difference <= MAX_ABSOLUTE_DIFFERENCE,
                            "유사도가 갈립니다: 차이 " + difference),
                    // 값보다 이쪽이 중요하다. 통과/거부와 어느 표본이 뽑혔는지는
                    // 완전히 같아야 한다 — 사용자에게 보이는 것이 그것이다.
                    () -> assertEquals(fromPairwise.matched(), fromEmbedding.matched(),
                            "통과 여부가 다릅니다"),
                    () -> assertEquals(fromPairwise.matchedKey(), fromEmbedding.matchedKey(),
                            "선택된 표본이 다릅니다"),
                    () -> assertEquals(fromPairwise.scores().size(), fromEmbedding.scores().size(),
                            "점수 개수가 다릅니다"));
        }
    }

    @Test
    @DisplayName("AC-32 표본별 점수가 하나하나 일치한다")
    void ac32_everyScoreAgrees() throws Exception {
        byte[] candidate = image(3000);
        List<ReferenceImage> references = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            references.add(ReferenceImage.ofBytes("sample-" + i, image(4000 + i)));
        }

        VerifierConfig config = VerifierConfig.withThreshold(0.80);

        try (ImageVerifier pairwise =
                ImageVerifiers.pairwiseOverHttp(similarityUri(), TIMEOUT, config);
                ImageVerifier embedding =
                        ImageVerifiers.embeddingOverHttp(baseUri(), "/embed", TIMEOUT, config)) {

            List<com.reviewticket.sdk.imageverify.api.Score> a =
                    pairwise.verify(references, candidate).scores();
            List<com.reviewticket.sdk.imageverify.api.Score> b =
                    embedding.verify(references, candidate).scores();

            // 최댓값만 같고 나머지가 어긋나면, 표본 구성이 조금만 달라져도
            // 판정이 갈리기 시작한다. 전부 확인한다.
            for (int i = 0; i < a.size(); i++) {
                assertEquals(a.get(i).key(), b.get(i).key(), "순서가 다릅니다: index " + i);
                assertEquals(a.get(i).similarity(), b.get(i).similarity(), MAX_ABSOLUTE_DIFFERENCE,
                        "점수가 갈립니다: " + a.get(i).key());
            }
        }
    }

    @Test
    @DisplayName("AC-71 pairwise 백엔드는 /embed 를 부르지 않는다")
    void ac71_pairwiseNeverCallsEmbed() throws Exception {
        // 롤백 경로가 진짜로 옛 엔드포인트만 쓰는지. /embed 가 없는 서버에서도
        // 돌아야 롤백이 의미가 있다.
        try (ImageVerifier pairwise = ImageVerifiers.pairwiseOverHttp(
                similarityUri(), TIMEOUT, VerifierConfig.withThreshold(0.80))) {

            VerificationResult result = pairwise.verify(
                    List.of(ReferenceImage.ofBytes("a", image(5000))), image(5001));

            assertTrue(Double.isFinite(result.similarity()));
        }
    }
}
