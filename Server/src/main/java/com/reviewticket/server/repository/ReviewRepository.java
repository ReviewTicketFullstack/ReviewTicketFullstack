package com.reviewticket.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.reviewticket.server.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 완전히 동일한 파일이 이미 올라왔는지. 인덱스 조회라 싸다. */
    Optional<Review> findByImageSha256(String imageSha256);

    /**
     * pHash 는 해밍 거리로 비교해야 해서 인덱스를 쓸 수 없다. 값만 전부 가져와
     * 메모리에서 비교한다. 데모 규모(수백~수천 행)에서는 이 편이 단순하고 빠르다.
     * 행이 수십만으로 늘면 BK-tree 나 버킷 분할이 필요해진다.
     */
    @Query("select r.imagePhash from Review r")
    List<Long> findAllPhashes();
}
