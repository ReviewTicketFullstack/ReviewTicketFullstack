package com.reviewticket.server.review;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.Food;
import com.reviewticket.server.domain.RejectionReason;

import jakarta.annotation.PostConstruct;

/**
 * 거부된 업로드를 사람이 읽을 수 있는 텍스트 파일로 남긴다. 날짜별로 한 파일에
 * 계속 덧붙인다 (2026-07-29.log).
 *
 * DB 의 ai_rejections 와 중복이지만 목적이 다르다 — DB 는 집계·분석용이고
 * 이 파일은 폴더를 열어 바로 눈으로 확인하기 위한 것이다. 둘 중 하나가 없어도
 * 서비스는 돌아간다.
 *
 * 파일 쓰기가 실패해도 절대 예외를 올리지 않는다. 로그를 못 남긴 것 때문에
 * 사용자 요청이 실패하면 본말이 전도된다.
 */
@Component
public class FailureLogWriter {

    private static final Logger log = LoggerFactory.getLogger(FailureLogWriter.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path root;

    public FailureLogWriter(ReviewTicketProperties properties) {
        this.root = Path.of(properties.failureLogDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureDirectoryExists() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            log.warn("거부 로그 폴더를 만들 수 없다: {}", root, e);
        }
    }

    public void append(Food food, RejectionReason reason, String predicted,
            Double pNonFood, Map<String, Double> probs, String sha256, long phash) {
        LocalDateTime now = LocalDateTime.now();
        Path file = root.resolve(now.format(DATE) + ".log");

        String line = "%s  %-14s  주문=%-6s  AI판정=%-13s  P(non_food)=%s  phash=%d  sha256=%s  probs=%s%n"
                .formatted(
                        now.format(TIME),
                        reason.name(),
                        food.getNameKo(),
                        predicted == null ? "-" : predicted,
                        pNonFood == null ? "-" : "%.6f".formatted(pNonFood),
                        phash,
                        sha256,
                        probs == null ? "-" : formatProbs(probs));

        try {
            writeHeaderIfNew(file);
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("거부 로그를 쓸 수 없다: {}", file, e);
        }
    }

    private void writeHeaderIfNew(Path file) throws IOException {
        if (Files.exists(file)) {
            return;
        }
        String header = """
                # ReviewTicket 업로드 거부 로그
                #
                #   not_food       음식 사진이 아님 (P(non_food) >= tau). 메뉴 비교까지 가지 않는다
                #   menu_mismatch  음식은 맞지만 주문한 메뉴와 다름
                #   duplicate      이미 올라온 사진. AI 를 부르지 않으므로 확률이 없다
                #
                # 사진 파일은 저장하지 않는다. 남는 건 해시와 확률뿐이다.
                # 같은 내용이 DB 의 ai_rejections 테이블에도 들어간다.

                """;
        Files.writeString(file, header, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String formatProbs(Map<String, Double> probs) {
        return probs.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> "%s=%.4f".formatted(e.getKey(), e.getValue()))
                .reduce((a, b) -> a + " " + b)
                .orElse("-");
    }
}
