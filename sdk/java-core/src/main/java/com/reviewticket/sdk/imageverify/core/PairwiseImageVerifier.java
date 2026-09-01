package com.reviewticket.sdk.imageverify.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.Score;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;
import com.reviewticket.sdk.imageverify.spi.PairwiseModel;

/**
 * 기본 판정 구현.
 *
 * <p>기준 이미지 전부와 <b>동시에</b> 대조한다. 순차로 부르면 추론 응답 시간이
 * 장수만큼 쌓여, 표본 5장이면 실측 기준 15초를 넘긴다. 동시에 쏘면 전체 소요가
 * 가장 느린 한 번 수준으로 유지된다.
 *
 * <p>스레드풀은 이 객체가 소유한다(ARCH-R7). 부르는 쪽에 동시성이 새어나가지
 * 않게 하려는 것이고, 그래서 {@link #close()} 로 반납하는 것도 이쪽 책임이다.
 */
public final class PairwiseImageVerifier implements ImageVerifier {

    private final PairwiseModel model;
    private final VerifierConfig config;
    private final ExecutorService executor;

    public PairwiseImageVerifier(PairwiseModel model, VerifierConfig config) {
        this.model = Objects.requireNonNull(model, "model 이 null 입니다");
        this.config = Objects.requireNonNull(config, "config 가 null 입니다");
        this.executor = Executors.newFixedThreadPool(config.parallelism(), namedDaemonFactory());
    }

    @Override
    public VerificationResult verify(List<ReferenceImage> references, byte[] candidate) {
        Verifications.validate(references, candidate, config);

        // 요청을 먼저 전부 띄우고 그다음에 거둔다. 띄우면서 바로 join 하면
        // 순차 실행이 되어 버린다.
        List<CompletableFuture<Score>> pending = new ArrayList<>(references.size());
        for (ReferenceImage reference : references) {
            pending.add(CompletableFuture.supplyAsync(
                    () -> new Score(reference.key(), model.similarity(candidate, reference.bytes())),
                    executor));
        }

        return Verifications.resultOf(collect(pending), config);
    }

    /**
     * 입력 순서대로 결과를 거둔다.
     *
     * <p>하나라도 실패하면 전체가 실패한다. 부분 성공을 허용해 성공한 것들의
     * 최댓값을 쓰면, 통과했어야 할 사진이 조용히 거부되어 사용자에게는 "다른
     * 것을 찍었다"로 보인다. 장애는 장애로 드러나는 편이 낫다.
     */
    private static List<Score> collect(List<CompletableFuture<Score>> pending) {
        List<Score> scores = new ArrayList<>(pending.size());
        try {
            for (CompletableFuture<Score> future : pending) {
                scores.add(future.join());
            }
        } catch (CompletionException e) {
            throw unwrap(e);
        }
        return scores;
    }

    /**
     * 스레드 안에서 던져진 예외는 CompletionException 으로 감싸여 나온다.
     * 부르는 쪽이 원래 타입을 알아볼 수 있게 벗겨서 다시 던진다.
     */
    private static RuntimeException unwrap(CompletionException wrapper) {
        Throwable cause = wrapper.getCause();
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new InferenceUnavailableException("추론 중 예기치 못한 오류가 발생했습니다", cause);
    }

    @Override
    public void close() {
        executor.shutdown();
        // 백엔드가 자원을 들고 있으면(HTTP 연결 등) 같이 반납한다. 이 객체가
        // 조립 시점에 넘겨받아 수명을 함께하는 구성 요소라 여기서 닫는다.
        if (model instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 종료 중의 실패로 애플리케이션 종료를 막지 않는다.
            }
        }
    }

    private static ThreadFactory namedDaemonFactory() {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "imageverify-" + sequence.getAndIncrement());
            // 데몬으로 둔다 — close() 를 부르지 않은 사용자의 JVM 이 안 죽는 것보다
            // 낫다. 정상 경로에서는 close() 가 불린다.
            thread.setDaemon(true);
            return thread;
        };
    }
}
