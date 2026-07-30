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
     * 메일 링크가 향하는 곳. 브라우저가 직접 여는 주소이므로 JSON 대신
     * 사람이 읽을 HTML 을 돌려준다. 인증 대기 화면이 폴링으로 상태를
     * 감지하고 있으므로, 이 창은 닫으라고 안내하면 흐름이 이어진다.
     */
    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public String verify(@RequestParam("token") @NotBlank String token) {
        String email = escapeHtml(authService.verify(token));
        return """
                <!doctype html>
                <html lang="ko"><head><meta charset="utf-8">
                <title>회원가입 완료</title></head>
                <body style="font-family:sans-serif;padding:2rem">
                  <h1>이메일 인증이 완료되어 회원가입이 끝났습니다</h1>
                  <p>%s</p>
                  <p>이 창을 닫고 회원가입 화면으로 돌아가 주세요.</p>
                </body></html>
                """.formatted(email);
    }

    /**
     * 이 컨트롤러가 유일하게 HTML 을 직접 만들어 내려주는 곳이라, 값을 그대로
     * 끼워 넣으면 삽입 통로가 된다. 이메일은 사용자가 입력한 문자열이고
     * &#64;Email 검증은 꽤 관대해서 특수문자가 통과할 여지가 있다.
     * 템플릿 엔진을 쓰지 않으므로 직접 escape 한다.
     */
    private static String escapeHtml(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** 인증 대기 화면이 짧은 주기로 물어본다. */
    @GetMapping("/status")
    public VerifiedResponse status(@RequestParam("email") @NotBlank String email) {
        return new VerifiedResponse(authService.isVerified(email));
    }

    // ---------- 로그인 ----------

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        return new LoginResponse(result.token(), result.expiresInSeconds(),
                result.user().getId(), result.user().getDisplayName(), result.user().getRole());
    }
}
