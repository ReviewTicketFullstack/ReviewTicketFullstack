package com.reviewticket.sdk.imageverify.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.Score;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;

/**
 * 판정 구현들이 공유하는 규칙.
 *
 * <p>백엔드(쌍 단위든 임베딩이든)가 달라도 입력 검사와 결과를 만드는 방식은
 * 같아야 한다. 그 부분이 갈라지면 백엔드를 바꿨을 때 판정이 조용히 달라진다 —
 * 그게 바로 Phase 2 가 일어나지 않게 하려는 일이다.
 */
final class Verifications {

    private Verifications() {
    }

    /**
     * 입력 계약 검사(SPEC §3.1). 위반은 부르는 쪽의 버그이므로 SDK 예외가 아니라
     * IllegalArgumentException 이다.
     *
     * <p>바이트가 비었는지는 보지 않는다 — 그걸 보려면 loader 를 지금 불러야 하고,
     * 그러면 지연 로딩이 무너진다. 그 검사는 실제로 읽는 시점에
     * {@link ReferenceImage#bytes()} 가 한다.
     */
    static void validate(List<ReferenceImage> references, byte[] candidate, VerifierConfig config) {
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

    /**
     * 점수 목록에서 결과를 만든다.
     *
     * <p>최고 점수를 고르되 동점이면 입력 순서상 앞선 것을 유지한다 — 부등호가
     * {@code >} 인 이유가 그것이다. 저장되는 "어느 기준과 맞았는가"가 실행마다
     * 흔들리면 안 된다.
     */
    static VerificationResult resultOf(List<Score> scores, VerifierConfig config) {
        Score best = scores.get(0);
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i).similarity() > best.similarity()) {
                best = scores.get(i);
            }
        }

        return new VerificationResult(
                best.similarity() >= config.matchThreshold(),
                best.similarity(),
                best.key(),
                config.matchThreshold(),
                scores);
    }
}
