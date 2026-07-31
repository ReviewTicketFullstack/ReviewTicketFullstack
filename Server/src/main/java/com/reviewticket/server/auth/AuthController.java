package com.reviewticket.server.auth;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reviewticket.server.domain.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ---------- 요청/응답 ----------

    /**
     * 비밀번호를 두 번 받는다. 서버에서도 일치를 확인하는 이유 — 프론트 검증은
     * 우회할 수 있고, 오타가 그대로 저장되면 본인도 로그인할 수 없게 된다.
     */
    public record SignUpRequest(
            @Email(message = "이메일 형식이 올바르지 않습니다") @NotBlank String email,
            @NotBlank(message = "비밀번호를 입력해 주세요") String password,
            @NotBlank(message = "비밀번호 확인을 입력해 주세요") String passwordConfirm,
            @NotNull(message = "사장님인지 고객인지 선택해 주세요") Role role,
            @NotBlank(message = "닉네임 또는 가게 이름을 입력해 주세요") String displayName) {
    }

    /**
     * 회원 번호를 돌려주지 않는다 — 이 시점에는 회원이 아직 만들어지지 않았다.
     * 인증 링크를 눌러야 생성된다.
     */
    public record SignUpResponse(String email, String message) {
    }

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password) {
    }

    public record LoginResponse(String token, long expiresInSeconds, long userId,
            String displayName, Role role) {
    }

    public record AvailabilityResponse(boolean available, String message) {
    }

    public record VerifiedResponse(boolean verified) {
    }

    public record VerifyRequest(@NotBlank(message = "토큰이 없습니다") String token) {
    }

    public record VerifyResponse(String email) {
    }

    public record ResetRequest(@Email(message = "이메일 형식이 올바르지 않습니다") @NotBlank String email) {
    }

    public record ResetTokenResponse(boolean valid) {
    }

    public record ResetConfirm(
            @NotBlank(message = "토큰이 없습니다") String token,
            @NotBlank(message = "새 비밀번호를 입력해 주세요") String newPassword,
            @NotBlank(message = "새 비밀번호 확인을 입력해 주세요") String newPasswordConfirm) {
    }

    public record MessageResponse(String message) {
    }

    // ---------- 중복 검사 ----------

    @GetMapping("/check-email")
    public AvailabilityResponse checkEmail(@RequestParam("email") @NotBlank String email) {
        boolean taken = authService.emailTaken(email);
        return new AvailabilityResponse(!taken, taken ? "이미 가입된 이메일입니다" : "사용할 수 있습니다");
    }

    /** 고객 닉네임과 사장 가게 이름이 같은 이름 공간이라 API 도 하나다. */
    @GetMapping("/check-name")
    public AvailabilityResponse checkName(@RequestParam("name") @NotBlank String name) {
        boolean taken = authService.displayNameTaken(name);
        return new AvailabilityResponse(!taken, taken ? "이미 쓰이고 있는 이름입니다" : "사용할 수 있습니다");
    }

    // ---------- 가입 ----------

    /**
     * 가입 요청. 회원을 만들지 않고 대기 표에 넣은 뒤 인증 메일을 보낸다.
     * 실제 회원 생성은 {@link #verify(String)} 에서 일어난다.
     */
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 서로 다릅니다");
        }
        authService.requestSignUp(request.email(), request.password(),
                request.role(), request.displayName());
        return new SignUpResponse(request.email().trim().toLowerCase(),
                "인증 메일을 보냈습니다. 메일의 링크를 눌러야 회원가입이 완료됩니다.");
    }

    @PostMapping("/resend")
    public void resend(@RequestParam("email") @NotBlank String email) {
        authService.resendVerification(email);
    }

    /**
     * 메일 링크가 여는 페이지. GET 은 부작용이 없다 — 버튼을 눌러 POST 할 때만
     * 회원이 만들어진다. 메일 클라이언트가 링크를 미리 열어도 가입되지 않는다.
     */
    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public String verifyPage(@RequestParam("token") @NotBlank String token) {
        return AuthPages.signupVerify(token);
    }

    /** 위 페이지의 버튼이 부른다. 여기서 실제 회원이 생성된다. */
    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public VerifyResponse verify(@Valid @RequestBody VerifyRequest request) {
        String email = authService.verify(request.token());
        return new VerifyResponse(email);
    }

    /** 인증 대기 화면이 짧은 주기로 물어본다. */
    @GetMapping("/status")
    public VerifiedResponse status(@RequestParam("email") @NotBlank String email) {
        return new VerifiedResponse(authService.isVerified(email));
    }

    // ---------- 비밀번호 재설정 ----------

    /** 재설정 요청. 이메일 존재 여부와 무관하게 항상 200 (열거 방지). */
    @PostMapping(value = "/password-reset/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MessageResponse requestPasswordReset(@Valid @RequestBody ResetRequest request) {
        authService.requestPasswordReset(request.email());
        return new MessageResponse("입력하신 이메일이 가입돼 있다면 재설정 메일을 보냈습니다.");
    }

    /** 재설정 메일 링크가 여는 페이지. GET 은 부작용 없음. */
    @GetMapping(value = "/password-reset", produces = MediaType.TEXT_HTML_VALUE)
    public String passwordResetPage(@RequestParam("token") @NotBlank String token) {
        return AuthPages.passwordReset(token);
    }

    /** 페이지의 인증 버튼이 부른다. 토큰 유효성만 확인하고 아무것도 바꾸지 않는다. */
    @GetMapping("/password-reset/check")
    public ResetTokenResponse checkResetToken(@RequestParam("token") @NotBlank String token) {
        return new ResetTokenResponse(authService.isResetTokenUsable(token));
    }

    /** 페이지의 변경 버튼이 부른다. 여기서 실제 비밀번호가 바뀐다. */
    @PostMapping(value = "/password-reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MessageResponse resetPassword(@Valid @RequestBody ResetConfirm request) {
        authService.resetPassword(request.token(), request.newPassword(), request.newPasswordConfirm());
        return new MessageResponse("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
    }

    // ---------- 로그인 ----------

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        return new LoginResponse(result.token(), result.expiresInSeconds(),
                result.user().getId(), result.user().getDisplayName(), result.user().getRole());
    }
}
