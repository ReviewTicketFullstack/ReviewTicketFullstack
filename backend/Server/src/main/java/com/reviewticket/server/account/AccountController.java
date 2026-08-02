package com.reviewticket.server.account;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reviewticket.server.auth.AuthAttemptLogger;
import com.reviewticket.server.domain.AuthAction;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인한 본인의 정보 조회와 수정.
 *
 * 대상 회원 번호를 요청에서 받지 않는다 — 토큰의 주체가 곧 대상이다.
 * 번호를 받으면 남의 번호를 넣어보는 시도를 막는 검사가 따로 필요해진다.
 */
@RestController
@RequestMapping("/api/me")
public class AccountController {

    private final AccountService accountService;
    private final AuthAttemptLogger attemptLogger;

    public AccountController(AccountService accountService, AuthAttemptLogger attemptLogger) {
        this.accountService = accountService;
        this.attemptLogger = attemptLogger;
    }

    public record MeResponse(long userId, String email, String displayName, Role role) {
    }

    public record ChangeNameRequest(@NotBlank(message = "이름을 입력해 주세요") String displayName) {
    }

    public record ChangeNameResponse(String displayName) {
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    @PatchMapping(value = "/name", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChangeNameResponse changeName(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChangeNameRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        try {
            String changed = accountService.changeDisplayName(user.getId(), request.displayName());
            attemptLogger.record(AuthAction.NICKNAME_CHANGE, user.getEmail(), request.displayName(), ip, true);
            return new ChangeNameResponse(changed);
        } catch (RuntimeException e) {
            attemptLogger.record(AuthAction.NICKNAME_CHANGE, user.getEmail(), request.displayName(), ip, false);
            throw e;
        }
    }

    // 비밀번호 변경은 /api/auth/password-reset 흐름으로 옮겼다.
    // 비밀번호를 잊은 사용자가 로그인 없이 재설정하는 것이 실제 요구였다.
}
