package com.reviewticket.server.auth;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.reviewticket.server.config.ReviewTicketProperties;

@Configuration
public class SecurityConfig {

    /**
     * BCrypt. 기본 강도(10)를 그대로 쓴다 — 로그인 한 번에 약 50~100ms 로,
     * 무차별 대입을 충분히 느리게 만들면서 사용자가 체감하지는 않는다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 프론트엔드를 다른 출처에서 띄울 때만 의미가 있다. Spring 이 /demo 로
     * 서빙하는 경우는 같은 출처라 CORS 자체가 개입하지 않는다.
     *
     * allowCredentials 를 켜지 않는다 — 쿠키를 쓰지 않고 Authorization 헤더로만
     * 인증하기 때문이다. 끄면 출처 패턴에 * 를 쓸 수 있고, 브라우저가 쿠키를
     * 자동으로 실어 보내지 않으므로 CSRF 도 성립하지 않는다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(ReviewTicketProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = properties.cors() == null ? null : properties.cors().allowedOrigins();
        config.setAllowedOriginPatterns(origins == null || origins.isEmpty() ? List.of("http://localhost:*") : origins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // 프론트가 응답 헤더를 읽을 일은 아직 없다. 필요해지면 여기 추가한다.
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource))
                // 세션을 쓰지 않으므로 CSRF 토큰이 필요 없다. CSRF 는 브라우저가
                // 쿠키를 자동으로 실어 보내기 때문에 성립하는 공격이고, 우리는
                // Authorization 헤더를 코드로 직접 붙인다.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 기본 로그인 폼과 브라우저 인증 팝업을 끈다. API 서버이므로
                // 인증 실패는 리다이렉트가 아니라 401 로 나가야 한다.
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // 가입, 로그인, 중복 검사, 이메일 인증은 토큰 없이 들어온다
                        .requestMatchers("/api/auth/**").permitAll()
                        // 프로토타입 화면과 메뉴 목록은 공개
                        .requestMatchers("/demo/**", "/", "/favicon.ico").permitAll()
                        .requestMatchers("/api/foods").permitAll()
                        // 리뷰 업로드는 아직 로그인 전 구조로 동작한다.
                        // 주문 기능을 붙일 때 인증 필요로 바꾼다.
                        .requestMatchers("/api/reviews").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
