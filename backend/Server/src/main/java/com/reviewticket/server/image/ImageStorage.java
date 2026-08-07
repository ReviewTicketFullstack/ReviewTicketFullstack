package com.reviewticket.server.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.reviewticket.server.config.ReviewTicketProperties;

/**
 * 이미지를 디스크에 저장/조회한다. reviewticket.upload.dir 아래에 파일로 두고,
 * URL 은 upload.base-url + "/" + 파일명 형태다. 이 base-url 경로를
 * WebMvcConfigurer(UploadStaticConfig)가 그대로 서빙한다.
 */
@Component
public class ImageStorage {

    private final ReviewTicketProperties properties;

    public ImageStorage(ReviewTicketProperties properties) {
        this.properties = properties;
    }

    /** 저장하고 URL 을 돌려준다. */
    public String save(byte[] bytes) {
        try {
            Path dir = Path.of(properties.upload().dir());
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ".jpg";
            Files.write(dir.resolve(filename), bytes);
            return properties.upload().baseUrl() + "/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장 실패", e);
        }
    }

    /** save() 가 돌려준 URL 로 원본 바이트를 다시 읽는다. AI 비교용 메뉴 표본 사진을 읽을 때 쓴다. */
    public byte[] read(String url) {
        try {
            String filename = url.substring(properties.upload().baseUrl().length() + 1);
            return Files.readAllBytes(Path.of(properties.upload().dir()).resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("이미지 읽기 실패: " + url, e);
        }
    }
}
