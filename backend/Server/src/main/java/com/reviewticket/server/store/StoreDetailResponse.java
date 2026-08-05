package com.reviewticket.server.store;

import java.util.List;

/** 주문 화면용 가게 상세. 목록과 같은 정보에 메뉴 배열이 붙는다. */
public record StoreDetailResponse(
        Long id,
        String name,
        String imageUrl,
        double rating,
        int reviewCount,
        List<MenuResponse> menus) {
}
