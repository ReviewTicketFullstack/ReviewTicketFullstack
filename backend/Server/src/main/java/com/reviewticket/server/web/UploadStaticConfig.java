package com.reviewticket.server.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.reviewticket.server.config.ReviewTicketProperties;

/**
 * reviewticket.upload.dir 에 저장한 로고/리뷰 사진을 upload.base-url 경로로 서빙한다.
 *
 * DemoStaticConfig 와 같은 이유로 별도 클래스로 둔다 — 프로토타입 정적 서빙과
 * 업로드 파일 서빙은 서로 다른 폴더, 다른 캐시 정책을 가질 수 있어 한 클래스에
 * 묶으면 나중에 헷갈린다.
 */
@Configuration
public class UploadStaticConfig implements WebMvcConfigurer {

    private final ReviewTicketProperties properties;

    public UploadStaticConfig(ReviewTicketProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseUrl = properties.upload().baseUrl();
        registry.addResourceHandler(baseUrl + "/**")
                .addResourceLocations("file:" + properties.upload().dir() + "/");
    }
}
