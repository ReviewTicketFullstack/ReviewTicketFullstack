package com.reviewticket.server.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * 두 가지 해시를 낸다. 성격이 정반대라 둘 다 저장한다.
 *
 *   sha256 - 바이트가 1비트라도 다르면 값이 통째로 바뀐다. 완전히 같은 파일만 잡는다.
 *   pHash  - 그림의 모양을 본다. 재압축·리사이즈·밝기 보정에는 거의 흔들리지 않는다.
 *
 * pHash 는 표준 DCT 방식이다:
 *   32x32 흑백 축소 -> 2D DCT -> 좌상단 8x8 (DC 제외) -> 중앙값보다 크면 1
 * 결과는 64비트. 두 해시의 다른 비트 수(해밍 거리)가 작으면 같은 사진으로 본다.
 *
 * 라이브러리를 쓰지 않고 직접 구현한 이유 — 임계값(거리 5)이 Python 구현으로
 * 잰 값이라, 구현이 다르면 그 숫자가 그대로 옮겨온다는 보장이 없다.
 * 직접 구현하고 우리 사진으로 다시 재는 편이 확실하다 (ImageHasherTest 참고).
 */
@Component
public class ImageHasher {

    private static final int DCT_SIZE = 32;
    private static final int HASH_SIDE = 8;

    /** DCT 계수 표. 크기가 고정이라 한 번만 만들어 재사용한다. */
    private static final double[][] COS = buildCosTable();

    private static double[][] buildCosTable() {
        double[][] table = new double[DCT_SIZE][DCT_SIZE];
        for (int u = 0; u < DCT_SIZE; u++) {
            for (int x = 0; x < DCT_SIZE; x++) {
                table[u][x] = Math.cos((2.0 * x + 1) * u * Math.PI / (2.0 * DCT_SIZE));
            }
        }
        return table;
    }

    public String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 이 없는 JVM 은 없다", e);
        }
    }

    /**
     * @return 64비트 지각 해시. 부호 있는 long 으로 다룬다 (MySQL BIGINT 도 signed).
     */
    public long pHash(BufferedImage image) {
        double[][] gray = toGray32(image);
        double[][] dct = dct2d(gray);

        // 좌상단 8x8 이 그림의 저주파(큰 형태) 성분. DC(0,0)는 전체 밝기라 뺀다.
        double[] coefficients = new double[HASH_SIDE * HASH_SIDE - 1];
        int n = 0;
        for (int u = 0; u < HASH_SIDE; u++) {
            for (int v = 0; v < HASH_SIDE; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }
                coefficients[n++] = dct[u][v];
            }
        }

        double median = median(coefficients);

        long hash = 0L;
        int bit = 0;
        for (int u = 0; u < HASH_SIDE; u++) {
            for (int v = 0; v < HASH_SIDE; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }
                if (dct[u][v] > median) {
                    hash |= (1L << bit);
                }
                bit++;
            }
        }
        return hash;
    }

    /** 다른 비트 수. 0 이면 사실상 같은 그림, 클수록 다른 그림. */
    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    private static double[][] toGray32(BufferedImage source) {
        BufferedImage small = new BufferedImage(DCT_SIZE, DCT_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, DCT_SIZE, DCT_SIZE, null);
        g.dispose();

        double[][] gray = new double[DCT_SIZE][DCT_SIZE];
        var raster = small.getRaster();
        for (int y = 0; y < DCT_SIZE; y++) {
            for (int x = 0; x < DCT_SIZE; x++) {
                gray[y][x] = raster.getSample(x, y, 0);
            }
        }
        return gray;
    }

    private static double[][] dct2d(double[][] input) {
        // 행 방향 DCT 후 열 방향 DCT. 좌상단 8x8 만 쓰지만 전체를 구해도 32x32 라 싸다.
        double[][] rows = new double[DCT_SIZE][DCT_SIZE];
        for (int y = 0; y < DCT_SIZE; y++) {
            for (int u = 0; u < DCT_SIZE; u++) {
                double sum = 0;
                for (int x = 0; x < DCT_SIZE; x++) {
                    sum += input[y][x] * COS[u][x];
                }
                rows[y][u] = sum * alpha(u);
            }
        }

        double[][] out = new double[DCT_SIZE][DCT_SIZE];
        for (int u = 0; u < DCT_SIZE; u++) {
            for (int v = 0; v < DCT_SIZE; v++) {
                double sum = 0;
                for (int y = 0; y < DCT_SIZE; y++) {
                    sum += rows[y][v] * COS[u][y];
                }
                out[u][v] = sum * alpha(u);
            }
        }
        return out;
    }

    private static double alpha(int u) {
        return u == 0 ? Math.sqrt(1.0 / DCT_SIZE) : Math.sqrt(2.0 / DCT_SIZE);
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 0
                ? (sorted[mid - 1] + sorted[mid]) / 2.0
                : sorted[mid];
    }
}
