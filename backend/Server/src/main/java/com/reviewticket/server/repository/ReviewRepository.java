package com.reviewticket.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.reviewticket.server.domain.Review;
import com.reviewticket.server.domain.Store;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderId(Long orderId);

    /**
     * 가게에 달린 리뷰 전부, 최신순. 6.2(고객이 보는 목록)와 6.3(사장 리뷰관리)이
     * 같은 쿼리를 쓴다 — 어떤 필드를 응답에 실을지만 서비스에서 갈린다.
     *
     * menu, user 를 함께 읽는 이유 — 응답에 menuName, displayName 이 필요한데
     * 지연 로딩으로 두면 리뷰 건수만큼 추가 쿼리가 나간다(N+1).
     */
    @Query("select r from Review r join fetch r.menu join fetch r.user where r.store = :store order by r.createdAt desc")
    List<Review> findByStoreOrderByCreatedAtDesc(@Param("store") Store store);

    /** GET /api/orders 의 reviewStatus="done" 판정용. 이 중 리뷰가 이미 달린 order id 만 골라낸다. */
    @Query("select r.order.id from Review r where r.order.id in :orderIds")
    List<Long> findOrderIdsWithReview(@Param("orderIds") List<Long> orderIds);
}
