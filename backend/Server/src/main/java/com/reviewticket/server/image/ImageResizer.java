package com.reviewticket.server.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

/**
 * 이미지 리사이즈. JDK 내장 ImageIO 만 쓴다(jpeg, png). 늘리지 않고 줄이기만
 * 한다 — 원본이 목표 크기보다 작으면 그대로 돌려준다.
 *
 * webp 는 아직 못 읽는다. ImageIO 가 기본으로 지원하지 않아 플러그인(예:
 * TwelveMonkeys imageio-webp)이 있어야 하는데, 이 환경에서 실제로 빌드해
 * 검증하지 못해 넣지 않았다. 지금은 jpeg, png 만 실제로 동작한다 — webp를
 * 진짜 지원하려면 그 의존성을 추가하고 빌드로 확인한 뒤 SUPPORTED_TYPES 에
 * "image/webp" 를 더하면 된다.
 */
@Component
public class ImageResizer {

    public static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png");

    public record Resized(byte[] bytes, int width, int height) {
    }

    /**
     * 긴 변이 targetLongEdge 보다 크면 그 크기로 줄인다. 업스케일은 하지 않는다.
     *
     * 줄일 필요가 없어도 원본이 JPEG 가 아니면 JPEG 로 다시 인코딩한다 —
     * ImageStorage 가 저장할 때 파일명에 무조건 .jpg 를 붙이므로, 내용이 PNG 인
     * 채로 통과하면 확장자와 실제 형식이 어긋난 파일이 남는다.
     */
    public Resized resize(byte[] original, int targetLongEdge) {
        BufferedImage source = read(original);
        int width = source.getWidth();
        int height = source.getHeight();
        int longEdge = Math.max(width, height);

        if (longEdge <= targetLongEdge && isJpeg(original)) {
            return new Resized(original, width, height);
        }

        double scale = longEdge <= targetLongEdge ? 1.0 : (double) targetLongEdge / longEdge;
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        // TYPE_INT_RGB 에는 투명 채널이 없다. 흰색으로 먼저 채우지 않으면 PNG 의
        // 투명 영역이 검게 뭉개져 AI 판정까지 틀어진다.
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newWidth, newHeight);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(scaled, "jpg", out)) {
                throw new IllegalStateException("JPEG 인코더를 찾지 못했습니다");
            }
            return new Resized(out.toByteArray(), newWidth, newHeight);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 인코딩 실패", e);
        }
    }

    /** 매직 넘버(FF D8 FF)로 JPEG 인지 본다. 확장자나 Content-Type 은 위조될 수 있다. */
    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    /** 리사이즈 없이 긴 변만 알고 싶을 때(최소 크기 검사). */
    public int longEdge(byte[] original) {
        BufferedImage source = read(original);
        return Math.max(source.getWidth(), source.getHeight());
    }

    private BufferedImage read(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다", e);
        }
    }
}
