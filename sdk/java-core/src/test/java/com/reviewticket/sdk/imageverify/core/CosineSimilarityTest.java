package com.reviewticket.sdk.imageverify.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

    private final CosineSimilarity cosine = new CosineSimilarity();

    @Test
    @DisplayName("같은 벡터는 1, 반대 벡터는 -1, 직교하면 0")
    void knownValues() {
        float[] a = { 1f, 0f, 0f };
        float[] opposite = { -1f, 0f, 0f };
        float[] orthogonal = { 0f, 1f, 0f };

        assertAll(
                () -> assertEquals(1.0, cosine.between(a, a), 1e-12),
                () -> assertEquals(-1.0, cosine.between(a, opposite), 1e-12),
                () -> assertEquals(0.0, cosine.between(a, orthogonal), 1e-12));
    }

    @Test
    @DisplayName("크기가 달라도 방향이 같으면 1이다")
    void magnitudeDoesNotMatter() {
        assertEquals(1.0, cosine.between(new float[] { 3f, 4f }, new float[] { 30f, 40f }), 1e-12);
    }

    @Test
    @DisplayName("길이가 다른 벡터는 거절한다")
    void lengthMismatchIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> cosine.between(new float[] { 1f }, new float[] { 1f, 2f }));
    }

    @Test
    @DisplayName("영벡터에도 터지지 않는다")
    void zeroVectorDoesNotDivideByZero() {
        double value = cosine.between(new float[] { 0f, 0f }, new float[] { 1f, 1f });
        assertTrue(Double.isFinite(value), "0으로 나눠 NaN/Infinity 가 나왔습니다: " + value);
    }

    @Test
    @DisplayName("정규화된 벡터에서는 내적과 같다")
    void matchesDotProductForNormalizedVectors() {
        // 서버가 L2 정규화해 보내므로 실제로 이 경로를 탄다. 노름으로 나누는
        // 과정이 값을 흔들지 않는지 확인한다.
        Random random = new Random(42);
        for (int trial = 0; trial < 20; trial++) {
            float[] a = normalized(random, 768);
            float[] b = normalized(random, 768);

            double dot = 0.0;
            for (int i = 0; i < a.length; i++) {
                dot += (double) a[i] * b[i];
            }

            // 정확히 같지는 않다. float32 로 정규화한 벡터의 노름은 딱 1.0 이
            // 아니라서(엡실론 약 1.2e-7), 노름으로 한 번 더 나누는 코사인이
            // 내적과 그만큼 갈린다. 실측 상대오차가 4e-8 수준이므로 BC-2 가
            // 요구하는 1e-5 보다 두 자릿수 여유가 있다.
            assertEquals(dot, cosine.between(a, b), 1e-6,
                    "정규화된 벡터에서 코사인이 내적과 지나치게 어긋납니다");
        }
    }

    private static float[] normalized(Random random, int dimension) {
        float[] vector = new float[dimension];
        double norm = 0.0;
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) random.nextGaussian();
            norm += (double) vector[i] * vector[i];
        }
        float length = (float) Math.sqrt(norm);
        for (int i = 0; i < dimension; i++) {
            vector[i] /= length;
        }
        return vector;
    }
}
