package com.reviewticket.sdk.imageverify.api;

/**
 * 판정 동작을 정하는 불변 설정.
 *
 * <p>Spring 을 모른다(ARCH-R1). Spring 사용자는 Starter 가 프로퍼티를 바인딩해
 * 이 객체를 만들어 주고, 그렇지 않은 사용자는 직접 만든다.
 *
 * <p>추론 서버 주소와 타임아웃은 여기 없다 — 그건 백엔드 구현의 관심사라
 * 백엔드를 만들 때 넘긴다. 이 객체는 "어떻게 판정할지"만 담는다.
 *
 * @param matchThreshold 이 값 이상이면 통과. 유사도와 같은 척도여야 한다
 * @param parallelism    기준 이미지를 동시에 몇 장까지 처리할지
 * @param maxReferences  한 번의 verify 에 넘길 수 있는 기준 이미지 수 상한
 */
public record VerifierConfig(double matchThreshold, int parallelism, int maxReferences) {

    public static final double DEFAULT_MATCH_THRESHOLD = 0.80;
    public static final int DEFAULT_PARALLELISM = 5;
    public static final int DEFAULT_MAX_REFERENCES = 16;

    public VerifierConfig {
        // NaN 도 함께 걸러내려고 부정형으로 쓴다 — NaN 은 어떤 비교에도 false 라
        // (t < -1 || t > 1) 형태로는 통과해 버린다.
        if (!(matchThreshold >= -1.0 && matchThreshold <= 1.0)) {
            throw new ConfigurationException(
                    "matchThreshold 는 -1.0 에서 1.0 사이여야 합니다: " + matchThreshold);
        }
        if (parallelism < 1) {
            throw new ConfigurationException("parallelism 은 1 이상이어야 합니다: " + parallelism);
        }
        if (maxReferences < 1) {
            throw new ConfigurationException("maxReferences 는 1 이상이어야 합니다: " + maxReferences);
        }
    }

    public static VerifierConfig defaults() {
        return new VerifierConfig(DEFAULT_MATCH_THRESHOLD, DEFAULT_PARALLELISM, DEFAULT_MAX_REFERENCES);
    }

    /** 임계값만 바꾸고 나머지는 기본값으로 둔다. 가장 흔한 사용 형태다. */
    public static VerifierConfig withThreshold(double matchThreshold) {
        return new VerifierConfig(matchThreshold, DEFAULT_PARALLELISM, DEFAULT_MAX_REFERENCES);
    }
}
