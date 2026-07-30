package com.reviewticket.server.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.reviewticket.server.config.ReviewTicketProperties;

import jakarta.annotation.PostConstruct;

/**
 * 승인된 사진만 디스크에 쓴다.
 *
 * 거부된 사진은 여기까지 오지 않는다 — 메모리에서 그냥 버려지므로 지울 임시
 * 파일이 애초에 생기지 않는다. 디스크 쓰기/삭제 왕복도 없다.
 *
 * DB 에는 경로만 넣는다. 이미지 바이트를 DB 에 넣으면 백업과 조회가 전부
 * 무거워진다.
 */
@Component
public class ImageStorage {

    private final Path root;

    public ImageStorage(ReviewTicketProperties properties) {
        this.root = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureDirectoryExists() throws IOException {
        Files.createDirectories(root);
    }

    /** 아직 쓰지 않고 이름만 정한다. DB 에 넣을 경로를 미리 알아야 하기 때문. */
    public String newFileName() {
        return UUID.randomUUID() + ".jpg";
    }

    public Path resolve(String fileName) {
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root)) {
            // 파일명이 조작돼 업로드 폴더 밖을 가리키는 걸 막는다
            throw new IllegalArgumentException("업로드 폴더 밖의 경로: " + fileName);
        }
        return path;
    }

    public void write(String fileName, byte[] jpegBytes) throws IOException {
        Files.write(resolve(fileName), jpegBytes,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    /** 저장 후 DB 커밋이 실패했을 때 되돌린다. */
    public void deleteQuietly(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (Exception ignored) {
            // 지우기 실패는 서비스를 막을 이유가 못 된다. 고아 파일로 남을 뿐이다.
        }
    }

    public Path root() {
        return root;
    }
}
