package com.reviewticket.server.upload;

/** POST /api/uploads 응답. 어느 표에도 대응하지 않는다 — 행을 만들지 않고 파일만 남기기 때문이다. */
public record UploadResponse(String url, int width, int height) {
}
