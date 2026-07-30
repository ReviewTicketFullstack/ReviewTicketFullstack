package com.reviewticket.server.account;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;

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

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    public record MeResponse(long userId, String email, String displayName, Role role) {
    }

    public record ChangeNameRequest(@NotBlank(message = "이름을 입력해 주세요") String displayName) {
    }

    public record ChangeNameResponse(String displayName) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "기존 비밀번호를 입력해 주세요") String currentPassword,
            @NotBlank(message = "새 비밀번호를 입력해 주세요") String newPassword,
            @NotBlank(message = "새 비밀번호 확인을 입력해 주세요") String newPasswordConfirm) {
    }

    /** 비밀번호를 바꾸면 옛 토큰이 죽으므로 새 토큰을 함께 내려준다. */
    public record ChangePasswordResponse(String token, String message) {
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    @PatchMapping(value = "/name", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChangeNameResponse changeName(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChangeNameRequest request) {
        String changed = accountService.changeDisplayName(user.getId(), request.displayName());
        return new ChangeNameResponse(changed);
    }

    @PatchMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChangePasswordResponse changePassword(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        String newToken = accountService.changePassword(user.getId(),
                request.currentPassword(), request.newPassword(), request.newPasswordConfirm());
        return new ChangePasswordResponse(newToken,
                "비밀번호를 변경했습니다. 다른 기기에서는 다시 로그인해야 합니다.");
    }
}
