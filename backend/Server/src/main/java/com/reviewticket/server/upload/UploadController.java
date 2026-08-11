package com.reviewticket.server.upload;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.image.ImageResizer;
import com.reviewticket.server.image.ImageStorage;

/**
 * 이미지 업로드. 가게 로고, 메뉴 대표 사진, 메뉴 표본 사진이 이 API 를 쓴다
 * (각 PATCH 가 이 응답의 url 을 그대로 받는다). 리뷰 사진은 쓰지 않는다 —
 * 판정에 실패한 사진을 남기면 안 되는데, 여기서 먼저 저장해 두면 실패할
 * 때마다 주인 없는 파일이 쌓인다(ReviewService 참고).
 *
 * 최소 크기는 용도에 따라 다르다. 기본은 제한 없음이다 — 로고나 목록 썸네일은
 * 작아도 화면에 지장이 없다. 다만 <b>메뉴 표본 사진은 제한을 걸어야 한다</b>.
 * 표본은 손님이 올린 리뷰 사진과 AI 가 대조하는 기준이라, 저화질이면 같은
 * 음식을 찍어도 유사도가 낮게 나와 멀쩡한 리뷰가 거부된다. 리뷰 사진 쪽은
 * 이미 1920 이상을 요구하는데(minImageLongEdge) 기준이 되는 표본만 아무
 * 크기나 받고 있었다 — 실제로 750px 짜리 표본이 등록돼 유사도가 문턱값을
 * 겨우 0.011 넘긴 적이 있다.
 *
 * 그래서 호출하는 쪽이 minLongEdge 로 필요한 하한을 정한다.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageResizer resizer;
    private final ImageStorage storage;
    private final ReviewTicketProperties properties;

    public UploadController(ImageResizer resizer, ImageStorage storage, ReviewTicketProperties properties) {
        this.resizer = resizer;
        this.storage = storage;
        this.properties = properties;
    }

    /**
     * @param minLongEdge 긴 변의 하한(px). 표본 사진처럼 화질이 판정에 영향을
     *                    주는 용도에서만 넘긴다. 없으면 크기를 보지 않는다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "minLongEdge", required = false) Integer minLongEdge) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("FILE_REQUIRED", "파일이 없습니다");
        }
        if (!ImageResizer.SUPPORTED_TYPES.contains(file.getContentType())) {
            throw new ValidationException("UNSUPPORTED_IMAGE_TYPE", "jpeg, png 가 아닙니다");
        }

        byte[] bytes = readBytes(file);
        if (minLongEdge != null && resizer.longEdge(bytes) < minLongEdge) {
            throw new ValidationException("IMAGE_TOO_SMALL",
                    "긴 변이 " + minLongEdge + "px 보다 작습니다");
        }

        ImageResizer.Resized resized = resizer.resize(bytes, properties.upload().targetLongEdge());
        String url = storage.save(resized.bytes());
        return new UploadResponse(url, resized.width(), resized.height());
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ValidationException("FILE_REQUIRED", "파일을 읽을 수 없습니다");
        }
    }
}
