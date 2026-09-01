package com.reviewticket.sdk.imageverify.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
public final class DefaultImageVerifier implements ImageVerifier {

    private final PairwiseModel model;
    private final VerifierConfig config;
    private final ExecutorService executor;

    public DefaultImageVerifier(PairwiseModel model, VerifierConfig config) {
        this.model = Objects.requireNonNull(model, "model 이 null 입니다");
        this.config = Objects.requireNonNull(config, "config 가 null 입니다");
        this.executor = Executors.newFixedThreadPool(config.parallelism(), namedDaemonFactory());
    }

    @Override
    public VerificationResult verify(List<ReferenceImage> references, byte[] candidate) {
        validate(references, candidate);

        // 요청을 먼저 전부 띄우고 그다음에 거둔다. 띄우면서 바로 join 하면
        // 순차 실행이 되어 버린다.
        List<CompletableFuture<Score>> pending = new ArrayList<>(references.size());
        for (ReferenceImage reference : references) {
            pending.add(CompletableFuture.supplyAsync(
                    () -> new Score(reference.key(), model.similarity(candidate, reference.bytes())),
                    executor));
        }

        List<Score> scores = collect(pending);
        Score best = best(scores);

        return new VerificationResult(
                best.similarity() >= config.matchThreshold(),
                best.similarity(),
                best.key(),
                config.matchThreshold(),
                scores);
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

    /**
     * 최고 점수. 동점이면 입력 순서상 앞선 것을 유지한다 — 부등호가 {@code >} 인
     * 이유가 그것이다. 저장되는 "어느 기준과 맞았는가"가 실행마다 흔들리면 안 된다.
     */
    private static Score best(List<Score> scores) {
        Score best = scores.get(0);
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i).similarity() > best.similarity()) {
                best = scores.get(i);
            }
        }
        return best;
    }

    /**
     * 입력 계약 검사(SPEC §3.1). 위반은 부르는 쪽의 버그이므로 SDK 예외가 아니라
     * IllegalArgumentException 이다.
     *
     * <p>바이트가 비었는지는 여기서 보지 않는다 — 그걸 보려면 loader 를 지금
     * 불러야 하고, 그러면 지연 로딩이 무너진다. 그 검사는 실제로 읽는 시점에
     * {@link ReferenceImage#bytes()} 가 한다.
     */
    private void validate(List<ReferenceImage> references, byte[] candidate) {
        if (references == null) {
            throw new IllegalArgumentException("references 가 null 입니다");
        }
        if (references.isEmpty()) {
            // 조용히 "불일치"로 처리하지 않는다. 기준 이미지가 없는 상태는 부르는
            // 쪽에서 이미 걸러졌어야 하는 상황이고, 그 검사가 사라졌다면 알아야 한다.
            throw new IllegalArgumentException("기준 이미지가 한 장도 없습니다");
        }
        if (references.size() > config.maxReferences()) {
            throw new IllegalArgumentException("기준 이미지가 너무 많습니다: "
                    + references.size() + " (상한 " + config.maxReferences() + ")");
        }
        if (candidate == null || candidate.length == 0) {
            throw new IllegalArgumentException("후보 이미지가 비어 있습니다");
        }

        Set<String> seen = new HashSet<>();
        for (ReferenceImage reference : references) {
            if (reference == null) {
                throw new IllegalArgumentException("기준 이미지 목록에 null 이 있습니다");
            }
            if (!seen.add(reference.key())) {
                throw new IllegalArgumentException("기준 이미지의 key 가 중복입니다: " + reference.key());
            }
        }
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
