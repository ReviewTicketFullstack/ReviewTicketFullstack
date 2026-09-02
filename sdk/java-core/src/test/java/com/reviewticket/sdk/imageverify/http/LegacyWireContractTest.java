package com.reviewticket.sdk.imageverify.http;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.reviewticket.sdk.imageverify.spi.PairwiseModel;

/**
 * 진짜 파이썬 추론 서버를 상대로 도는 계약 테스트.
 *
 * <p>기본 빌드에서는 건너뛴다 — 서버가 떠 있어야 하므로 CI 에서 항상 돌릴 수 없다.
 * 켜려면 주소를 준다:
 *
 * <pre>
 *   ./gradlew test -Dimageverify.contract.url=http://127.0.0.1:8000/similarity
 * </pre>
 *
 * <p>이 테스트가 있는 이유 — 나머지 HTTP 테스트는 JDK 스텁 서버를 상대로 도는데,
 * 그 스텁은 본문을 원시 바이트로 읽을 뿐 <b>multipart 파싱을 하지 않는다</b>.
 * 즉 손으로 만든 {@link MultipartBody} 를 파이썬의 python-multipart 가 실제로
 * 받아들이는지는 스텁으로 증명되지 않는다. 그 구멍을 메우는 자리다.
 */
@EnabledIfSystemProperty(named = "imageverify.contract.url", matches = ".+")
class LegacyWireContractTest {

    /**
     * 진짜 JPEG 를 만들고, 그 뒤에 multipart 파서를 헷갈리게 할 만한 열을 덧붙인다.
     *
     * <p>추론 서버는 받은 바이트를 실제로 디코드하므로 아무 잡음이나 보내면 400 이
     * 돌아온다 — 그러면 전송 형식이 아니라 이미지 형식을 시험하게 된다.
     *
     * <p>함정 바이트는 EOI 마커 뒤에 붙인다. 디코더는 이미지가 끝난 뒤의 내용을
     * 무시하므로 그림으로서는 멀쩡하면서, 본문에는 경계 문자열과 헷갈릴 열이
     * 실제로 들어간다. sha256 대조는 이 뒷부분까지 포함해 이뤄진다.
     */
    private static byte[] noisyBytes(long seed, int length) throws Exception {
        BufferedImage canvas = new BufferedImage(48, 48, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(seed);
        for (int y = 0; y < 48; y++) {
            for (int x = 0; x < 48; x++) {
                canvas.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(canvas, "jpg", out), "JPEG 인코더를 찾지 못했습니다");
        out.write("\r\n--boundary\r\n".getBytes(StandardCharsets.ISO_8859_1));

        byte[] tail = new byte[Math.max(0, length)];
        random.nextBytes(tail);
        out.write(tail);
        return out.toByteArray();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @Test
    @DisplayName("계약: 실제 파이썬 서버가 손으로 만든 multipart 본문을 그대로 받는다")
    void pythonServerParsesOurMultipartBody() throws Exception {
        URI endpoint = URI.create(System.getProperty("imageverify.contract.url"));

        byte[] candidate = noisyBytes(1, 4096);
        byte[] reference = noisyBytes(2, 8192);

        // 서버가 돌려주는 sha256 을 읽으려면 응답 전체가 필요하다. similarity 만
        // 뽑는 정식 경로와 별개로, 여기서는 본문을 직접 확인한다.
        try (HttpPairwiseModel model = new HttpPairwiseModel(endpoint, Duration.ofSeconds(30))) {
            double similarity = model.similarity(candidate, reference);

            // 값이 무엇이든 상관없다 — 파싱되어 숫자가 돌아왔다는 것 자체가
            // 요청·응답 형식이 맞았다는 증거다.
            assertTrue(Double.isFinite(similarity), "similarity 를 읽지 못했습니다: " + similarity);
        }

        // 바이트가 상하지 않았는지는 서버가 계산한 해시로 확인한다.
        String body = RawPost.post(endpoint, candidate, reference);
        assertAll(
                () -> assertTrue(body.contains(sha256(candidate)),
                        "reviewImage 바이트가 서버에 그대로 도착하지 않았습니다"),
                () -> assertTrue(body.contains(sha256(reference)),
                        "compareImage 바이트가 서버에 그대로 도착하지 않았습니다"),
                () -> assertTrue(body.contains("review.jpg"), "reviewImage 파일명"),
                () -> assertTrue(body.contains("compare.jpg"), "compareImage 파일명"));
    }

    @Test
    @DisplayName("계약: 응답 키가 similarity 그대로다")
    void responseKeyIsStillSimilarity() throws Exception {
        URI endpoint = URI.create(System.getProperty("imageverify.contract.url"));
        String body = RawPost.post(endpoint, noisyBytes(3, 1024), noisyBytes(4, 1024));

        assertTrue(body.contains("\"similarity\""),
                "응답 키가 바뀌었습니다. 백엔드 계약이 깨집니다: " + body);
    }

    @Test
    @DisplayName("계약: 파트 이름이 reviewImage/compareImage 그대로다")
    void partNamesAreUnchanged() throws Exception {
        URI endpoint = URI.create(System.getProperty("imageverify.contract.url"));

        // 파트 이름이 틀리면 FastAPI 가 422 를 내고, 어댑터는 그걸
        // InvalidImageException 으로 바꾼다. 즉 예외 없이 끝났다는 것이
        // 파트 이름이 맞았다는 뜻이다.
        try (HttpPairwiseModel model = new HttpPairwiseModel(endpoint, Duration.ofSeconds(30))) {
            assertEquals(true, Double.isFinite(model.similarity(noisyBytes(5, 512), noisyBytes(6, 512))));
        }
    }
}
