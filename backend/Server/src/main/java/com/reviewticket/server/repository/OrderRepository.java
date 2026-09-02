package com.reviewticket.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.reviewticket.server.domain.Order;
import com.reviewticket.server.domain.Store;
import com.reviewticket.server.domain.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 내 주문 내역, 최신순.
     *
     * store 를 함께 읽는 이유 — 응답에 가게 이름이 들어가는데 지연 로딩으로
     * 두면 주문 건수만큼 추가 쿼리가 나간다(N+1).
     */
    @Query("select o from Order o join fetch o.store where o.customer = :customer order by o.id desc")
    List<Order> findMyOrders(@Param("customer") User customer);

    /**
     * 사장이 보는 "리뷰 대기중" 목록. 리뷰이벤트에 참여했는데 아직 리뷰가 없는 주문이다.
     * 만료 여부(pending/expired)는 응답을 만들 때 expireTime 을 보고 가른다 — 여기서는
     * 나누지 않는다, 사장 화면이 두 숫자를 같이 보여주기 때문이다.
     */
    @Query("select o from Order o join fetch o.customer where o.store = :store and o.reviewEventApply = true "
            + "and not exists (select 1 from Review r where r.order = o) order by o.orderedAt desc")
    List<Order> findPendingReviewOrders(@Param("store") Store store);
}
