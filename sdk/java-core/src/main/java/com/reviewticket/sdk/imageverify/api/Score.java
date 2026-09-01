package com.reviewticket.sdk.imageverify.api;

/**
 * 기준 이미지 한 장과의 대조 결과.
 *
 * @param key        기준 이미지의 key
 * @param similarity 유사도. 백엔드가 준 값 그대로이며 반올림하지 않는다
 */
public record Score(String key, double similarity) {
}
