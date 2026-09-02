package com.reviewticket.sdk.imageverify;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 소스 위생 검사 — 가드레일의 세 번째 겹.
 *
 * <p>{@code check-boundaries.sh} 는 사람이 실행해야 하고 CI 는 잊힐 수 있다.
 * 이 테스트는 {@code ./gradlew build} 를 도는 누구에게나 자동으로 걸린다.
 *
 * <p>검사 대상 경로는 빌드 스크립트가 시스템 프로퍼티로 넘겨준다 — 테스트는
 * build 디렉터리에서 돌아 자기 소스가 어디 있는지 스스로 알 수 없다.
 */
class SourceHygieneTest {

    private static List<Path> mainSources() {
        String root = System.getProperty("imageverify.core.src");
        assertTrue(root != null && !root.isBlank(),
                "imageverify.core.src 시스템 프로퍼티가 없습니다 — build.gradle 을 확인하세요");

        Path sourceRoot = Path.of(root);
        assertTrue(Files.isDirectory(sourceRoot), "소스 폴더가 없습니다: " + sourceRoot);

        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> forbiddenTokens() throws IOException {
        try (InputStream in = SourceHygieneTest.class.getResourceAsStream("/forbidden-model-tokens.txt")) {
            assertTrue(in != null, "forbidden-model-tokens.txt 를 찾지 못했습니다");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return text.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .toList();
        }
    }

    @Test
    @DisplayName("AC-52 java-core 에 특정 모델의 흔적이 없다 (ARCH-R4)")
    void ac52_noModelSpecificTokensInCore() throws IOException {
        List<String> tokens = forbiddenTokens();
        assertTrue(!tokens.isEmpty(), "금지 토큰 목록이 비었습니다 — 검사가 무력화됩니다");

        List<String> violations = new ArrayList<>();
        for (Path source : mainSources()) {
            String content = Files.readString(source).toLowerCase(Locale.ROOT);
            for (String token : tokens) {
                if (content.contains(token)) {
                    violations.add(source.getFileName() + " 에 '" + token + "'");
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("java-core 가 특정 모델을 알고 있습니다. 모델 식별자는 추론 서버가 "
                    + "알려 주는 값이어야 합니다:\n  " + String.join("\n  ", violations));
        }
    }

    @Test
    @DisplayName("java-core 에 Spring 의존이 없다 (ARCH-R1)")
    void noSpringInCore() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : mainSources()) {
            String content = Files.readString(source);
            if (content.contains("import org.springframework.") || content.contains("import jakarta.")) {
                violations.add(source.getFileName().toString());
            }
        }

        if (!violations.isEmpty()) {
            fail("java-core 에 Spring/jakarta 의존이 들어왔습니다: " + violations);
        }
    }

    @Test
    @DisplayName("검사가 실제로 파일을 보고 있다")
    void theScanActuallyReadsSources() {
        // 대상이 0개인데 초록으로 통과하는 것이 가드레일의 가장 흔한 고장 방식이다.
        assertTrue(mainSources().size() >= 5,
                "main 소스를 제대로 찾지 못했습니다: " + mainSources().size() + "개");
    }
}
