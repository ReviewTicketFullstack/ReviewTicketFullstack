package com.reviewticket.server.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.coobird.thumbnailator.Thumbnails;

/**
 * pHash 임계값을 우리 구현으로 다시 측정한다.
 *
 * 기존 실측(거리 5 기준)은 Python 구현으로 잰 값이라 Java 구현에 그대로
 * 옮겨온다는 보장이 없다. 여기서 같은 종류의 변형을 걸어 거리를 직접 재고,
 * '압축 계열은 작고 크롭/반전/무관한 사진은 크다'는 성질이 유지되는지 본다.
 */
class ImageHasherTest {

    private static final Path DATASET = Path.of("..", "..", "AI_Model", "dataset", "test");
    private static final int THRESHOLD = 5;

    private final ImageHasher hasher = new ImageHasher();

    private static byte[] imageBytes(String className, int index) throws IOException {
        try (var files = Files.list(DATASET.resolve(className))) {
            Path path = files.filter(Files::isRegularFile).sorted().skip(index).findFirst().orElseThrow();
            return Files.readAllBytes(path);
        }
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static byte[] reencode(BufferedImage image, double quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(image).scale(1.0).outputFormat("jpg").outputQuality(quality).toOutputStream(out);
        return out.toByteArray();
    }

    private static BufferedImage resized(BufferedImage image, int longEdge) throws IOException {
        return Thumbnails.of(image).size(longEdge, longEdge).keepAspectRatio(true).asBufferedImage();
    }

    private static BufferedImage brighter(BufferedImage source, float factor) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(source, 0, 0, null);
        return new RescaleOp(factor, 0, null).filter(rgb, null);
    }

    private static BufferedImage croppedBy(BufferedImage source, double ratio) {
        int dx = (int) (source.getWidth() * ratio);
        int dy = (int) (source.getHeight() * ratio);
        return source.getSubimage(dx, dy, source.getWidth() - 2 * dx, source.getHeight() - 2 * dy);
    }

    private static BufferedImage flippedHorizontally(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.drawImage(source, source.getWidth(), 0, 0, source.getHeight(),
                0, 0, source.getWidth(), source.getHeight(), null);
        g.dispose();
        return out;
    }

    @Test
    @DisplayName("변형별 pHash 거리를 재서 임계값 5가 우리 구현에서도 유효한지 확인")
    void measuresDistancesAcrossTransformations() throws IOException {
        byte[] originalBytes = imageBytes("pizza", 0);
        BufferedImage original = decode(originalBytes);
        long base = hasher.pHash(original);

        Map<String, Long> variants = new LinkedHashMap<>();
        variants.put("재저장만 (q95)", hasher.pHash(decode(reencode(original, 0.95))));
        variants.put("1080px 리사이즈 + q85", hasher.pHash(decode(reencode(resized(original, 1080), 0.85))));
        variants.put("JPEG 품질 50", hasher.pHash(decode(reencode(original, 0.50))));
        variants.put("JPEG 품질 20", hasher.pHash(decode(reencode(original, 0.20))));
        variants.put("밝기 +20%", hasher.pHash(brighter(original, 1.2f)));
        variants.put("가장자리 5% 크롭", hasher.pHash(croppedBy(original, 0.05)));
        variants.put("좌우 반전", hasher.pHash(flippedHorizontally(original)));
        variants.put("무관한 사진 (라멘)", hasher.pHash(decode(imageBytes("ramen", 0))));
        variants.put("무관한 사진 (건물)", hasher.pHash(decode(imageBytes("non_food", 0))));

        System.out.println("\n=== pHash 해밍 거리 (64비트 중 다른 비트 수), 임계값 " + THRESHOLD + " ===");
        variants.forEach((label, hash) -> {
            int distance = ImageHasher.hammingDistance(base, hash);
            System.out.printf("  %-24s %2d  %s%n", label, distance,
                    distance <= THRESHOLD ? "-> 같은 사진으로 판정" : "-> 다른 사진으로 판정");
        });
        System.out.println();

        // 압축·리사이즈·밝기 보정은 같은 사진으로 남아야 한다.
        assertThat(ImageHasher.hammingDistance(base, variants.get("재저장만 (q95)"))).isLessThanOrEqualTo(THRESHOLD);
        assertThat(ImageHasher.hammingDistance(base, variants.get("1080px 리사이즈 + q85"))).isLessThanOrEqualTo(THRESHOLD);
        assertThat(ImageHasher.hammingDistance(base, variants.get("JPEG 품질 50"))).isLessThanOrEqualTo(THRESHOLD);
        assertThat(ImageHasher.hammingDistance(base, variants.get("JPEG 품질 20"))).isLessThanOrEqualTo(THRESHOLD);
        assertThat(ImageHasher.hammingDistance(base, variants.get("밝기 +20%"))).isLessThanOrEqualTo(THRESHOLD);

        // 무관한 사진은 확실히 멀어야 한다.
        assertThat(ImageHasher.hammingDistance(base, variants.get("무관한 사진 (라멘)"))).isGreaterThan(THRESHOLD);
        assertThat(ImageHasher.hammingDistance(base, variants.get("무관한 사진 (건물)"))).isGreaterThan(THRESHOLD);
    }

    @Test
    @DisplayName("sha256 은 재저장만 해도 값이 통째로 바뀐다 — pHash 와 성격이 반대")
    void sha256ChangesOnAnyReencode() throws IOException {
        byte[] originalBytes = imageBytes("pizza", 0);
        byte[] reencoded = reencode(decode(originalBytes), 0.95);

        assertThat(hasher.sha256(originalBytes)).hasSize(64);
        assertThat(hasher.sha256(reencoded)).isNotEqualTo(hasher.sha256(originalBytes));
    }

    @Test
    @DisplayName("서로 다른 사진 20쌍의 거리가 모두 임계값보다 크다 (오탐 없음)")
    void distinctPhotosNeverCollide() throws IOException {
        long[] hashes = new long[20];
        String[] classes = { "pizza", "ramen", "hamburger", "bibimbap" };
        int n = 0;
        for (String className : classes) {
            for (int i = 0; i < 5; i++) {
                hashes[n++] = hasher.pHash(decode(imageBytes(className, i)));
            }
        }

        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < hashes.length; i++) {
            for (int j = i + 1; j < hashes.length; j++) {
                minDistance = Math.min(minDistance, ImageHasher.hammingDistance(hashes[i], hashes[j]));
            }
        }

        System.out.println("서로 다른 사진 20장 중 가장 가까운 쌍의 거리: " + minDistance);
        assertThat(minDistance).isGreaterThan(THRESHOLD);
    }
}
