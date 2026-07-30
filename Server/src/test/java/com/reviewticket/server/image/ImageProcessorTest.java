package com.reviewticket.server.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageProcessorTest {

    private static final Path DATASET = Path.of("..", "..", "AI_Model", "dataset", "test");

    private final ImageProcessor processor = new ImageProcessor();

    private static byte[] realPhoto() throws IOException {
        try (var files = Files.list(DATASET.resolve("pizza"))) {
            return Files.readAllBytes(files.filter(Files::isRegularFile).sorted().findFirst().orElseThrow());
        }
    }

    @Test
    @DisplayName("정상 사진은 긴 변이 1600 이하로 줄고 JPEG 로 나온다")
    void shrinksRealPhoto() throws IOException {
        ImageProcessor.Processed out = processor.process(realPhoto());

        assertThat(out.jpegBytes()).isNotEmpty();
        assertThat(Math.max(out.image().getWidth(), out.image().getHeight()))
                .isLessThanOrEqualTo(ImageProcessor.MAX_EDGE);
    }

    @Test
    @DisplayName("이미지가 아닌 바이트는 IOException 으로 실패해야 한다 (500 이 아니라 400 이 되도록)")
    void rejectsGarbageBytes() {
        byte[] garbage = "not an image at all".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> processor.process(garbage))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("빈 바이트도 IOException")
    void rejectsEmptyBytes() {
        assertThatThrownBy(() -> processor.process(new byte[0]))
                .isInstanceOf(IOException.class);
    }
}
