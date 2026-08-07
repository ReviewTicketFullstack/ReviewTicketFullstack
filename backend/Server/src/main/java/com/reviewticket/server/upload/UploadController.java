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
 * 이미지 업로드. 지금은 가게 로고에서만 쓴다(PATCH /api/stores/me 가 이 응답의
 * url 을 그대로 받는다). 리뷰 사진은 이 API 를 쓰지 않는다 — 판정에 실패한
 * 사진을 남기면 안 되는데, 여기서 먼저 저장해 두면 실패할 때마다 주인 없는
 * 파일이 쌓인다(ReviewService 참고).
 *
 * 최소 크기 제한을 두지 않는다 — 로고는 작아도 화면에 지장이 없고 AI 판정에도
 * 쓰이지 않는다. 리뷰 사진(minImageLongEdge)과 다른 점이다.
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("FILE_REQUIRED", "파일이 없습니다");
        }
        if (!ImageResizer.SUPPORTED_TYPES.contains(file.getContentType())) {
            throw new ValidationException("UNSUPPORTED_IMAGE_TYPE", "jpeg, png 가 아닙니다");
        }

        ImageResizer.Resized resized = resizer.resize(readBytes(file), properties.upload().targetLongEdge());
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
