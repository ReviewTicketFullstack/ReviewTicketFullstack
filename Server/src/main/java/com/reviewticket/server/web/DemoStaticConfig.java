package com.reviewticket.server.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.reviewticket.server.config.ReviewTicketProperties;

/**
 * 팀원이 만든 프로토타입(FrontEnd/Demo/index.html)을 Spring 이 직접 서빙한다.
 *
 *   http://localhost:8080/demo/
 *
 * 왜 이렇게 하나 — 그 프로토타입은 "의존성 없는 HTML 파일 하나"가 원칙이라
 * 빌드 도구를 끼우고 싶지 않다. 같은 출처에서 내려주면
 *   - CORS 설정이 아예 필요 없다 (file:// 로 열면 Origin 이 null 이라 지저분해진다)
 *   - Node 도 Vite 도 띄울 필요가 없다
 *   - 파일을 복사하지 않으므로 팀원이 저장소를 갱신하면 그대로 반영된다
 *
 * 캐시를 끄는 이유: 프로토타입을 고치는 중에 브라우저가 옛 파일을 붙잡고 있으면
 * 고친 게 반영되지 않아 헷갈린다.
 */
@Configuration
public class DemoStaticConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(DemoStaticConfig.class);

    private final Path demoDir;

    public DemoStaticConfig(ReviewTicketProperties properties) {
        this.demoDir = Path.of(properties.demoDir()).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!Files.isDirectory(demoDir)) {
            log.warn("데모 폴더가 없어 /demo 를 서빙하지 않는다: {}", demoDir);
            return;
        }
        log.info("데모 프로토타입 서빙: http://localhost:8080/demo/  <-  {}", demoDir);

        // Vite의 기본 빌드 결과는 JS/CSS를 /assets/... 와 /favicon.svg 로 참조한다.
        // /demo 아래의 index.html을 열었을 때에도 이 파일들이 같은 서버에서 제공되게 한다.
        registry.addResourceHandler("/demo/**", "/assets/**", "/favicon.svg")
                .addResourceLocations(demoDir.toUri().toString())
                .setCachePeriod(0);
    }

    /** 정적 폴더에는 기본 문서 개념이 없어서 /demo 와 /demo/ 를 직접 이어준다. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/demo").setViewName("redirect:/demo/index.html");
        registry.addViewController("/demo/").setViewName("forward:/demo/index.html");
    }
}
