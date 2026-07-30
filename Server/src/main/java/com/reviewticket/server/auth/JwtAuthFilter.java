package com.reviewticket.server.auth;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization: Bearer <토큰> 을 읽어 인증 주체를 세운다.
 *
 * 매 요청 DB 를 한 번 읽는다. "JWT 는 상태가 없어서 DB 를 안 본다"는 통념과
 * 어긋나지만 의도한 선택이다 — 토큰 버전을 대조해야 비밀번호 변경 즉시
 * 옛 토큰을 끊을 수 있고, 그 대조는 DB 의 현재 값이 있어야 가능하다.
 * 어차피 대부분의 API 가 회원 정보를 필요로 하므로 추가 비용은 사실상 없다.
 *
 * 토큰이 없거나 잘못돼도 여기서 401 을 내지 않는다. 인증 주체를 세우지
 * 않고 그냥 통과시키고, 보호된 경로라면 Security 설정이 401 을 낸다.
 * 그래야 공개 경로가 토큰 유무와 무관하게 동작한다.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            authenticate(token);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        JwtProvider.ParsedToken parsed = jwtProvider.parse(token);
        if (parsed == null) {
            return;
        }
        Optional<User> found = userRepository.findById(parsed.userId());
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();

        // 비밀번호가 바뀌면 서버의 버전이 올라가 옛 토큰은 여기서 걸러진다.
        if (parsed.tokenVersion() == null || parsed.tokenVersion() != user.getTokenVersion()) {
            return;
        }

        // 이메일 인증 전 계정은 로그인 자체가 막혀 있지만, 인증 전에 발급된
        // 토큰이 떠돌 여지를 남기지 않기 위해 여기서도 확인한다.
        if (!user.isEmailVerified()) {
            return;
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
