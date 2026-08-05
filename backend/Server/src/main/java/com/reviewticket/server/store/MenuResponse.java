package com.reviewticket.server.store;

/** @param price 원 단위 정수. 표시 형식(콤마, "원")은 화면이 맡는다 */
public record MenuResponse(
        Long id,
        String name,
        int price,
        String imageUrl,
        boolean reviewEvent) {
}
