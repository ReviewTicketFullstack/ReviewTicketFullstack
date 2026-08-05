package com.reviewticket.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.reviewticket.server.domain.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByStoreIdOrderByIdAsc(Long storeId);

    /**
     * 리뷰 배지를 띄울 가게들의 id.
     *
     * 가게마다 메뉴를 한 번씩 조회하면 목록 길이만큼 쿼리가 늘어나므로
     * (N+1), 배지가 붙는 가게 id 만 한 번에 받아 와서 메모리에서 맞춘다.
     */
    @Query("select distinct m.store.id from Menu m where m.reviewEvent = true and m.store.id in :storeIds")
    List<Long> findStoreIdsWithReviewEvent(@Param("storeIds") List<Long> storeIds);
}
