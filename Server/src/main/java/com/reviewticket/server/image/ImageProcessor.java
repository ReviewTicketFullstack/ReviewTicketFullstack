package com.reviewticket.server.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import net.coobird.thumbnailator.Thumbnails;

/**
 * 업로드된 사진을 저장할 한 벌로 만든다 — 긴 변 1600px, JPEG 품질 85 (약 280KB).
 * 원본은 남기지 않는다. 소비자가 둘뿐인데 둘 다 원본이 필요 없기 때문이다:
 * AI 는 내부에서 224px 로 줄여 보고, 화면은 전체화면 확대해도 1,200px 을 넘지 않는다.
 *
 * EXIF 회전을 반드시 반영해야 한다. 폰은 센서 방향 그대로 저장하고 회전을 EXIF
 * 태그로만 표시하는데, 태그를 버리면 사진이 90도 누워 저장된다. 모델은 flipud=0
 * 으로 학습해 뒤집힌 사진을 배우지 않았으므로 판정 정확도까지 같이 떨어진다.
 * Thumbnails.of(InputStream) 경로가 EXIF 방향을 적용해준다.
 */
@Component
public class ImageProcessor {

    public static final int MAX_EDGE = 1600;
    public static final double JPEG_QUALITY = 0.85;

    /** 축소된 JPEG 바이트와, 해시 계산에 쓸 디코딩 결과. */
    public record Processed(byte[] jpegBytes, BufferedImage image) {
    }

    public Processed process(byte[] original) throws IOException {
        int longestEdge = readLongestEdge(original);

        // 원본이 1600px 보다 작으면 늘리지 않는다. 늘려봐야 화질은 그대로고
        // 파일만 커진다.
        int target = Math.min(MAX_EDGE, longestEdge);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(original))
                .size(target, target)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(JPEG_QUALITY)
                .toOutputStream(out);

        byte[] jpegBytes = out.toByteArray();

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpegBytes));
        if (decoded == null) {
            throw new IOException("축소 결과를 디코딩할 수 없다");
        }
        return new Processed(jpegBytes, decoded);
    }

    /**
     * 헤더만 읽어 가로·세로 중 큰 값을 구한다. 픽셀을 디코딩하지 않으므로
     * 4K 사진이라도 25MB 비트맵이 뜨지 않는다.
     *
     * EXIF 로 90도 회전된 사진은 가로·세로가 뒤바뀌지만, 둘 중 '큰 쪽'은
     * 회전과 무관하게 같다. 그래서 이 값만으로 축소 여부를 판단해도 된다.
     */
    private int readLongestEdge(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("이미지로 읽을 수 없는 파일");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return Math.max(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }
}
