package com.reviewticket.server.store;

/**
 * 가게 상세(GET /api/stores/{storeId})에 실리는 메뉴 한 줄. 고객이 보는 형태라
 * storeId(바깥에 이미 있다), menuCreatedAt/menuLatestUpdate(쓸 곳이 없다)는 뺐다.
 *
 * @param menuPrice 원 단위 정수. 표시 형식(콤마, "원")은 화면이 맡는다
 */
public record MenuResponse(
        Long menuId,
        String menuName,
        int menuPrice,
        String menuImageUrl,
        boolean reviewEvent) {
}
