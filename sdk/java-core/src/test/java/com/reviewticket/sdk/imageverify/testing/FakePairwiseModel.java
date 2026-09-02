package com.reviewticket.sdk.imageverify.testing;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.reviewticket.sdk.imageverify.spi.PairwiseModel;

/**
 * 네트워크 없이 판정 로직만 돌리기 위한 백엔드.
 *
 * <p>이게 컴파일되고 동작한다는 사실 자체가 모델 교체 가능성의 증명이다 —
 * 추론 서버도 파이썬도 없이 SDK 전체 경로가 돈다(AC-50).
 */
public final class FakePairwiseModel implements PairwiseModel {

    // 큐가 아니라 배열 + 원자적 인덱스를 쓴다. 이 모델은 여러 스레드가 동시에
    // 부르는 것이 정상이고(그걸 확인하는 테스트가 있다), ArrayDeque 는 스레드
    // 안전하지 않아 동시 호출에서 조용히 어긋난다.
    private double[] scores = new double[0];
    private final AtomicInteger calls = new AtomicInteger();
    private Function<Integer, RuntimeException> failureAt;
    private int failIndex = -1;
    private long delayMillis;

    /** 호출 순서대로 이 점수들을 돌려준다. */
    public static FakePairwiseModel returning(double... values) {
        FakePairwiseModel model = new FakePairwiseModel();
        model.scores = values.clone();
        return model;
    }

    /** n번째(0-based) 호출에서 예외를 던진다. */
    public FakePairwiseModel failingAt(int index, Function<Integer, RuntimeException> failure) {
        this.failIndex = index;
        this.failureAt = failure;
        return this;
    }

    /** 호출마다 이만큼 지연시킨다. 동시성 확인용. */
    public FakePairwiseModel withDelay(long millis) {
        this.delayMillis = millis;
        return this;
    }

    public int callCount() {
        return calls.get();
    }

    @Override
    public String modelId() {
        return "fake";
    }

    @Override
    public double similarity(byte[] candidate, byte[] reference) {
        int index = calls.getAndIncrement();
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (index == failIndex) {
            throw failureAt.apply(index);
        }
        if (index >= scores.length) {
            throw new IllegalStateException("준비된 점수보다 많이 호출됐습니다: " + index);
        }
        return scores[index];
    }

    /**
     * 순서가 뒤섞이면 어느 점수가 어느 기준에 붙는지 알 수 없으므로, 순서에
     * 의존하는 테스트는 이 방식 대신 키별 고정 점수를 쓴다.
     */
    public static PairwiseModel byReferenceBytes(List<byte[]> order, double... values) {
        return new PairwiseModel() {
            @Override
            public String modelId() {
                return "fake-by-bytes";
            }

            @Override
            public double similarity(byte[] candidate, byte[] reference) {
                for (int i = 0; i < order.size(); i++) {
                    if (java.util.Arrays.equals(order.get(i), reference)) {
                        return values[i];
                    }
                }
                throw new IllegalStateException("모르는 기준 이미지입니다");
            }
        };
    }
}
