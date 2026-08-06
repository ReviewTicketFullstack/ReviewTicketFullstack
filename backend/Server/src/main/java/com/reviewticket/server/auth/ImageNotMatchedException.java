package com.reviewticket.server.auth;

/**
 * 리뷰 사진이 AI 유사도 문턱값을 넘지 못한 경우. 422 로 나간다.
 *
 * 400이 아니라 422(Unprocessable Entity)를 쓰는 이유 — 요청 형식은 멀쩡하고
 * (사진도, 나머지 필드도 다 정상), 그 내용을 처리할 수 없을 뿐이다.
 *
 * 정상 흐름이다. 찍은 사진이 주문한 메뉴와 다르면 늘 일어나는 결과이며
 * 재시도를 전제로 한다. 그래서 유사도 값을 응답에 함께 실어, 화면이
 * "일치율 62%"처럼 구체적인 수치로 안내할 수 있게 한다.
 */
public class ImageNotMatchedException extends RuntimeException {

    private final double imageSimilarity;

    public ImageNotMatchedException(double imageSimilarity) {
        super("리뷰 사진이 메뉴 표본 사진과 일치하지 않습니다: 유사도 " + imageSimilarity);
        this.imageSimilarity = imageSimilarity;
    }

    public double getImageSimilarity() {
        return imageSimilarity;
    }
}
