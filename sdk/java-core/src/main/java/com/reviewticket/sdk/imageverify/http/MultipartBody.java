package com.reviewticket.sdk.imageverify.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * multipart/form-data 본문을 손으로 만든다.
 *
 * <p>라이브러리를 쓰지 않는 이유는 java-core 의 외부 의존성을 0개로 유지하기
 * 위해서다(SPEC 결정 D-2). 우리가 보내는 것은 파일 파트 두 개뿐이라 규격의
 * 극히 일부만 있으면 된다.
 */
final class MultipartBody {

    private final String boundary;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private MultipartBody(String boundary) {
        this.boundary = boundary;
    }

    /**
     * 경계 문자열은 요청마다 새로 만든다. 고정값을 쓰면 이미지 바이트 안에
     * 우연히 같은 열이 들어 있을 때 본문이 깨진다.
     */
    static MultipartBody create() {
        return new MultipartBody("----imageverify" + UUID.randomUUID().toString().replace("-", ""));
    }

    String boundary() {
        return boundary;
    }

    String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    MultipartBody filePart(String name, String filename, byte[] content) {
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n");
        write("Content-Type: application/octet-stream\r\n\r\n");
        out.writeBytes(content);
        write("\r\n");
        return this;
    }

    byte[] build() {
        write("--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private void write(String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
}
