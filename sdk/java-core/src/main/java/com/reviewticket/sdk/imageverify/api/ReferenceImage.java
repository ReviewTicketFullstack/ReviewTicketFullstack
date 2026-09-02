package com.reviewticket.sdk.imageverify.api;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 대조 대상이 되는 기준 이미지 한 장.
 *
 * <p>바이트를 값이 아니라 {@link Supplier} 로 받는 것이 이 클래스의 핵심이다.
 * 그 덕에 SDK 는 이미지가 디스크에 있는지, S3 에 있는지, DB BLOB 인지 전혀
 * 모른 채로 동작한다 — 그 지식은 전부 람다 안에 갇힌다(ARCH-R5).
 *
 * <p>지연 로딩은 캐시의 전제이기도 하다. 임베딩이 캐시에 있으면 loader 를 아예
 * 부르지 않으므로, 바이트를 읽는 I/O 자체가 일어나지 않는다(Phase 3).
 *
 * <p><b>key 안정성 계약</b> — key 뒤의 바이트가 바뀌면 부르는 쪽은 반드시 새
 * key 를 써야 한다. SDK 는 내용이 바뀌었는지 알 방법이 없다. 이 계약이 깨지면
 * 캐시가 낡은 값으로 조용히 잘못된 판정을 내린다.
 */
public final class ReferenceImage {

    private final String key;
    private final Supplier<byte[]> loader;

    // loader 를 최대 한 번만 부르기 위한 보관함. 여러 스레드가 같은
    // ReferenceImage 를 볼 수 있어 volatile + double-checked locking 을 쓴다.
    private volatile byte[] loaded;

    private ReferenceImage(String key, Supplier<byte[]> loader) {
        this.key = requireUsableKey(key);
        this.loader = Objects.requireNonNull(loader, "loader 가 null 입니다");
    }

    /** 바이트를 필요할 때 읽는다. 저장소를 감추는 정상 경로다. */
    public static ReferenceImage of(String key, Supplier<byte[]> loader) {
        return new ReferenceImage(key, loader);
    }

    /** 이미 바이트를 들고 있을 때. 주로 테스트와 인메모리 사용처. */
    public static ReferenceImage ofBytes(String key, byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes 가 null 입니다");
        return new ReferenceImage(key, () -> bytes);
    }

    public String key() {
        return key;
    }

    /**
     * 바이트를 돌려준다. loader 는 최대 한 번만 불린다.
     *
     * @throws IllegalArgumentException loader 가 null 이나 빈 배열을 돌려준 경우
     */
    public byte[] bytes() {
        byte[] local = loaded;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (loaded == null) {
                byte[] fromLoader = loader.get();
                if (fromLoader == null || fromLoader.length == 0) {
                    throw new IllegalArgumentException(
                            "기준 이미지의 바이트가 비어 있습니다: key=" + key);
                }
                loaded = fromLoader;
            }
            return loaded;
        }
    }

    private static String requireUsableKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("기준 이미지의 key 가 비어 있습니다");
        }
        return key;
    }

    @Override
    public String toString() {
        return "ReferenceImage[" + key + "]";
    }
}
