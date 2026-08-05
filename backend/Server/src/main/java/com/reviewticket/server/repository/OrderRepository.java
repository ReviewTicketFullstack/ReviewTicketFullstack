package com.reviewticket.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.reviewticket.server.domain.Order;
import com.reviewticket.server.domain.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 내 주문 내역, 최신순.
     *
     * store 를 함께 읽는 이유 — 응답에 가게 이름이 들어가는데 지연 로딩으로
     * 두면 주문 건수만큼 추가 쿼리가 나간다(N+1).
     */
    @Query("select o from Order o join fetch o.store where o.user = :user order by o.id desc")
    List<Order> findMyOrders(@Param("user") User user);
}
