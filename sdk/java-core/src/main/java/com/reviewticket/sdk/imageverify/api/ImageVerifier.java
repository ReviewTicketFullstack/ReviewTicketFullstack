package com.reviewticket.sdk.imageverify.api;

import java.util.List;

/**
 * 후보 이미지가 기준 이미지 중 하나와 일치하는지 판정한다.
 *
 * <p>이 인터페이스 뒤에 추론 서버 주소, HTTP, 임베딩 모델, 유사도 계산, 캐싱이
 * 전부 숨는다. 부르는 쪽은 그중 무엇도 알 필요가 없다.
 *
 * <p>구현체는 스레드 안전해야 한다 — 하나를 만들어 애플리케이션 전체가 공유한다.
 */
public interface ImageVerifier extends AutoCloseable {

    /**
     * 후보 이미지를 기준 이미지 전부와 대조해 가장 높은 유사도와 통과 여부를 돌려준다.
     *
     * <p>임계값을 넘지 못한 것은 <b>예외가 아니라 결과다</b>. 불일치는 정상적으로
     * 자주 일어나는 판정이므로 {@link VerificationResult#matched()} 가 false 인
     * 결과로 돌아온다. 그것을 오류로 볼지는 부르는 쪽이 정한다.
     *
     * @param references 기준 이미지. 1장 이상이어야 하고 key 는 고유해야 한다
     * @param candidate  후보 이미지의 원본 바이트. 리사이즈·형식 변환을 하지 않는다
     * @throws IllegalArgumentException      입력 계약 위반 — 부르는 쪽의 버그다
     * @throws InferenceUnavailableException 추론 백엔드가 죽었거나 시간을 초과했다
     * @throws InvalidImageException         추론 백엔드가 이미지를 디코드하지 못했다
     */
    VerificationResult verify(List<ReferenceImage> references, byte[] candidate);

    /**
     * 기준 이미지를 미리 계산해 캐시에 채운다. 판정은 하지 않는다.
     *
     * <p>캐시가 없거나 꺼져 있으면 아무 일도 하지 않는다. 실제 효과는 Phase 3
     * 부터다 — 그전까지는 이 기본 구현대로 무시된다.
     */
    default void warmUp(List<ReferenceImage> references) {
    }

    /**
     * 들고 있던 자원(스레드풀, HTTP 연결)을 반납한다.
     *
     * <p>SDK 가 동시성을 소유하므로(ARCH-R7) 종료도 SDK 가 책임진다. Spring 은
     * 빈이 AutoCloseable 이면 소멸 시 이 메서드를 자동으로 부른다.
     *
     * <p>{@link AutoCloseable} 과 달리 검사 예외를 던지지 않는다 — 종료 처리 때문에
     * 부르는 쪽이 try-catch 를 쓰게 만들 이유가 없다.
     */
    @Override
    default void close() {
    }
}
