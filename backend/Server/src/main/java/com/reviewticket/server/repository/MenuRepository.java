package com.reviewticket.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByStoreIdOrderByIdAsc(Long storeId);

    /** 그 메뉴 중 하나라도 리뷰이벤트 대상이 있는지. 가게 생성 시 store.markReviewing 을 정하는 데 한 번 쓴다. */
    boolean existsByStoreIdAndReviewEventTrue(Long storeId);
}
