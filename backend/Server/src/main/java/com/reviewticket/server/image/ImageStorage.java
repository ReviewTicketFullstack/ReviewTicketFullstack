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

    /**
     * URL 을 실제 파일 경로로 바꿈다. 업로드 폴더 밖을 가리키는 값은 거절한다.
     *
     * 여기서 막지 않으면 "/uploads/../../..." 같은 값으로 서버 안 아무 파일이나
     * 읽힐 수 있다 — 표본 사진 주소는 사장이 직접 보낸 문자열이지 서버가 만든
     * 값이 아니다. save() 가 짓는 이름은 항상 UUID + ".jpg" 라 순수 파일명이면 충분하다.
     *
     * 문자열 검사와 정규화 후 경로 비교를 둘 다 한다 — 앞쪽을 빠져나가는 표기가
     * 있더라도 뒤쪽에서 한 번 더 걸리게 한다.
     */
    public Path pathOf(String url) {
        String prefix = properties.upload().baseUrl() + "/";
        if (url == null || !url.startsWith(prefix)) {
            throw new IllegalArgumentException("업로드 파일 주소가 아닙니다: " + url);
        }
        String filename = url.substring(prefix.length());
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("파일명이 아닙니다: " + url);
        }

        Path dir = Path.of(properties.upload().dir()).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir)) {
            throw new IllegalArgumentException("업로드 폴더 밖을 가리킵니다: " + url);
        }
        return file;
    }

    /**
     * 그 주소의 파일이 실제로 있는지. 표본 사진을 메뉴에 붙이기 전에 확인할 때 쓴다.
     *
     * 주소 형식이 이상해도 예외가 아니라 false 다 — 불러지는 쪽은 둘을 구분할 이유가
     * 없고, 구분해 알려 주면 서버에 어떤 파일이 있는지를 떠볼 수 있게 된다.
     */
    public boolean exists(String url) {
        try {
            return Files.isRegularFile(pathOf(url));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** save() 가 돌려준 URL 로 원본 바이트를 다시 읽는다. AI 비교용 메뉴 표본 사진을 읽을 때 쓴다. */
    public byte[] read(String url) {
        try {
            return Files.readAllBytes(pathOf(url));
        } catch (IOException e) {
            throw new IllegalStateException("이미지 읽기 실패: " + url, e);
        }
    }
}
