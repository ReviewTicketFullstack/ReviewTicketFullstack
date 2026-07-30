package com.reviewticket.server.review;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.reviewticket.server.repository.FoodRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
@Validated // 없으면 @RequestParam 에 붙인 @Min/@Size 가 무시된다
public class ReviewController {

    private static final int MIN_CONTENT_LENGTH = 10;

    private final ReviewService reviewService;
    private final FoodRepository foodRepository;

    public ReviewController(ReviewService reviewService, FoodRepository foodRepository) {
        this.reviewService = reviewService;
        this.foodRepository = foodRepository;
    }

    /** 개발용 메뉴 선택기가 쓸 목록. 로그인·주문이 붙으면 주문 정보로 대체된다. */
    public record FoodResponse(Long id, String name, String nameKo, int price) {
    }

    @GetMapping("/foods")
    public List<FoodResponse> foods() {
        return foodRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(f -> new FoodResponse(f.getId(), f.getName(), f.getNameKo(), f.getPrice()))
                .toList();
    }

    /**
     * 리뷰 제출. 거부도 정상적인 업무 결과이므로 HTTP 200 으로 내려간다
     * (approved 필드로 구분). 4xx 는 요청 자체가 잘못됐을 때만,
     * 503 은 추론 서버에 닿지 못했을 때만 쓴다.
     *
     * 아직 로그인이 없어서 사용자를 묻지 않는다. foodName 은 개발용 메뉴
     * 선택기가 보낸다 — 나중에는 주문에서 가져온다.
     */
    @PostMapping(value = "/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReviewSubmitResponse submit(
            @RequestParam("foodName") @NotBlank String foodName,
            @RequestParam("rating") @Min(1) @Max(5) int rating,
            @RequestParam("content") @Size(min = MIN_CONTENT_LENGTH, max = 1000) String content,
            @RequestParam("image") MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("사진이 비어 있습니다");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일이 아닙니다: " + contentType);
        }

        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("업로드를 읽을 수 없습니다", e);
        }

        return reviewService.submit(foodName, rating, content, bytes);
    }
}
