package com.reviewticket.server.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
// Boot 4 모듈화로 TestRestTemplate 이 spring-boot-resttestclient 로 옮겨졌다.
// Boot 3 의 org.springframework.boot.test.web.client 는 더 이상 없다.
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.reviewticket.server.ai.AiClient;
import com.reviewticket.server.image.ImageStorage;
import com.reviewticket.server.repository.AiRejectionRepository;
import com.reviewticket.server.repository.FoodRepository;
import com.reviewticket.server.repository.ReviewRepository;

/**
 * 업로드 API 전체를 진짜로 통과시킨다 — HTTP, 이미지 축소, 해시, FastAPI 호출,
 * 판정, 파일 저장, DB INSERT 까지. 목(mock)이 하나도 없다.
 *
 * 추론 서버가 떠 있어야 한다:
 *   cd C:\dev\ReviewTicket\AI_Model
 *   .venv-serve\Scripts\python.exe -m uvicorn src.serve:app --port 8000
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 실서비스 DB 를 비우지 않도록 reviewticket_test 로 붙는다
@ActiveProfiles({ "local", "test" })
@AutoConfigureTestRestTemplate // Boot 4 는 TestRestTemplate 을 자동 등록하지 않는다
class ReviewUploadEndToEndTest {

    private static final Path DATASET = Path.of("..", "..", "AI_Model", "dataset", "test");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AiClient aiClient;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AiRejectionRepository aiRejectionRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private com.reviewticket.server.config.ReviewTicketProperties properties;

    @Autowired
    private ImageStorage imageStorage;

    /** 테스트가 만든 것만 지운다. 시작·끝 양쪽에서 비운다. */
    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        for (var review : reviewRepository.findAll()) {
            Path path = Path.of(review.getImagePath());
            Files.deleteIfExists(path);
        }
        reviewRepository.deleteAll();
        aiRejectionRepository.deleteAll();
    }

    private static byte[] image(String className, int index) throws IOException {
        try (var files = Files.list(DATASET.resolve(className))) {
            Path path = files.filter(Files::isRegularFile).sorted().skip(index).findFirst().orElseThrow();
            return Files.readAllBytes(path);
        }
    }

    private ResponseEntity<Map> post(String foodName, int rating, String content, byte[] imageBytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("foodName", foodName);
        body.add("rating", rating);
        body.add("content", content);
        body.add("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return rest.postForEntity("http://localhost:" + port + "/api/reviews",
                new HttpEntity<>(body, headers), Map.class);
    }

    @Test
    @DisplayName("메뉴 목록 조회")
    void listsFoods() {
        ResponseEntity<List> response = rest.getForEntity(
                "http://localhost:" + port + "/api/foods", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(5);
    }

    @Test
    @DisplayName("피자 사진 + 피자 주문 -> 승인. 파일이 저장되고 DB 에 행이 생긴다")
    void approvesMatchingPhoto() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        ResponseEntity<Map> response = post("pizza", 5, "도우가 쫄깃하고 치즈가 많아요.", image("pizza", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body.get("approved")).isEqualTo(true);
        assertThat(body.get("predicted")).isEqualTo("pizza");
        assertThat(body.get("reviewId")).isNotNull();

        // DB
        assertThat(reviewRepository.count()).isEqualTo(1);
        var saved = reviewRepository.findAll().get(0);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getContent()).isEqualTo("도우가 쫄깃하고 치즈가 많아요.");
        assertThat(saved.getAiProbs()).hasSize(6);
        // expectedFood 는 LAZY 이고 open-in-view: false 라 트랜잭션 밖에서는
        // 필드를 읽을 수 없다 (그게 의도한 설정이다). id 는 프록시가 이미 갖고
        // 있어 초기화 없이 읽히므로 id 로 비교한다.
        assertThat(saved.getExpectedFood().getId())
                .isEqualTo(foodRepository.findByName("pizza").orElseThrow().getId());

        // 파일이 실제로 존재하고, 축소돼 있다
        Path stored = Path.of(saved.getImagePath());
        assertThat(stored).exists();
        assertThat(stored.startsWith(imageStorage.root())).isTrue();
        assertThat(Files.size(stored)).isPositive();
    }

    @Test
    @DisplayName("피자 사진 + 라멘 주문 -> 메뉴 불일치 거부. 파일은 남지 않는다")
    void rejectsMenuMismatch() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        ResponseEntity<Map> response = post("ramen", 4, "면발이 쫄깃했습니다 정말로.", image("pizza", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body.get("approved")).isEqualTo(false);
        assertThat(body.get("reason")).isEqualTo("menu_mismatch");
        assertThat(body.get("message")).isEqualTo("주문한 메뉴와 사진이 일치하지 않습니다");

        assertThat(reviewRepository.count()).isZero();
        assertThat(aiRejectionRepository.count()).isEqualTo(1);

        // 거부된 사진은 디스크에 쓰지 않는다
        try (var files = Files.list(imageStorage.root())) {
            assertThat(files.filter(p -> p.toString().endsWith(".jpg")).count()).isZero();
        }
    }

    @Test
    @DisplayName("건물 사진 -> 음식 아님 거부. 텍스트 로그 파일에도 한 줄 쌓인다")
    void rejectsNonFood() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        Path logFile = Path.of(properties.failureLogDir()).toAbsolutePath().normalize()
                .resolve(LocalDate.now() + ".log");
        long linesBefore = Files.exists(logFile) ? Files.readAllLines(logFile).size() : 0;

        ResponseEntity<Map> response = post("pizza", 3, "사진을 잘못 올렸습니다요.", image("non_food", 0));

        Map<?, ?> body = response.getBody();
        assertThat(body.get("approved")).isEqualTo(false);
        assertThat(body.get("reason")).isEqualTo("not_food");
        assertThat(body.get("message")).isEqualTo("음식 사진이 아닙니다");
        assertThat(reviewRepository.count()).isZero();

        assertThat(logFile).exists();
        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines.size()).isGreaterThan((int) linesBefore);
        assertThat(lines.getLast())
                .contains("not_food")
                .contains("주문=피자")
                .contains("non_food=");
    }

    @Test
    @DisplayName("같은 사진을 두 번 올리면 두 번째는 중복으로 거부. AI 를 부르지 않는다")
    void rejectsDuplicate() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        byte[] photo = image("pizza", 0);
        assertThat(post("pizza", 5, "처음 올리는 리뷰입니다.", photo).getBody().get("approved")).isEqualTo(true);

        ResponseEntity<Map> second = post("pizza", 5, "같은 사진을 또 올립니다.", photo);
        Map<?, ?> body = second.getBody();

        assertThat(body.get("approved")).isEqualTo(false);
        assertThat(body.get("reason")).isEqualTo("duplicate");
        // AI 를 부르지 않았으므로 확률이 없다
        assertThat(body.get("probs")).isNull();
        assertThat(reviewRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("별점이 범위를 벗어나면 400")
    void rejectsInvalidRating() throws IOException {
        ResponseEntity<Map> response = post("pizza", 6, "별점이 잘못된 리뷰입니다.", image("pizza", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("후기가 10자 미만이면 400")
    void rejectsShortContent() throws IOException {
        ResponseEntity<Map> response = post("pizza", 5, "짧음", image("pizza", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("Content-Type 은 image/jpeg 인데 내용이 이미지가 아니면 400 (500 이나 연결 끊김이 아니라)")
    void rejectsCorruptImageBytes() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("foodName", "pizza");
        body.add("rating", 5);
        body.add("content", "확장자만 jpg 인 파일을 올립니다.");
        body.add("image", new ByteArrayResource("not an image at all".getBytes()) {
            @Override
            public String getFilename() {
                return "fake.jpg";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = rest.postForEntity("http://localhost:" + port + "/api/reviews",
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 400")
    void rejectsNonImage() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("foodName", "pizza");
        body.add("rating", 5);
        body.add("content", "텍스트 파일을 올려봅니다.");
        body.add("image", new ByteArrayResource("not an image".getBytes()) {
            @Override
            public String getFilename() {
                return "notes.txt";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = rest.postForEntity("http://localhost:" + port + "/api/reviews",
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
