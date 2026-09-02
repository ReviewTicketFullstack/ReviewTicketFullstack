package com.reviewticket.server.auth;

/**
 * 비밀번호 형식 규칙. 대문자, 소문자, 숫자, 특수문자를 모두 포함하고 6~14자.
 * 예: Abc123!@
 *
 * 프론트에서도 같은 규칙으로 즉시 검사하지만, 여기서 다시 본다.
 * 프론트 검증은 사용자 편의를 위한 것이고 우회할 수 있다 — 규칙을
 * 실제로 강제하는 곳은 서버뿐이다.
 *
 * 길이와 허용 특수문자는 프론트의 passwordRegex 와 같은 값이어야 한다.
 * 서버가 더 엄격하면 화면 안내대로 입력한 사용자가 제출 단계에서 거부당한다
 * (이전에 서버만 8자 이상이라 6~7자가 그렇게 실패했다).
 *
 * 정규식 하나로 몰아넣지 않고 조건별로 나눈 이유: 어떤 조건이 빠졌는지
 * 사용자에게 알려줄 수 있다. "형식이 틀렸습니다"만 띄우면 뭘 고쳐야
 * 할지 몰라 같은 실수를 반복한다.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;

    /** 프론트와 같은 상한. BCrypt 한계(72바이트)보다 훨씬 짧아 잘림 걱정은 없다. */
    public static final int MAX_LENGTH = 14;

    /** 프론트 passwordRegex 가 허용하는 특수문자와 같은 집합. */
    private static final String SPECIALS = "!@#$%^&*";

    private PasswordPolicy() {
    }

    /** 어긋난 조건 하나를 (errorCode, message) 쌍으로 담는다. */
    public record Violation(String errorCode, String message) {
    }

    /**
     * 판정 순서는 가입 화면(SignUpForm 의 getPasswordError)과 같다 —
     * 대문자, 소문자, 숫자, 특수문자, 사용 불가 문자, 길이.
     *
     * 순서를 맞춰야 하는 이유: 어긋난 조건이 여럿이어도 문구는 맨 앞에서 걸린
     * 하나만 나간다. 서버가 길이를 먼저 보면, 같은 비밀번호를 두고 화면은
     * "대문자를 포함해주세요"라고 하는데 서버는 "6자 이상이어야 합니다"라고
     * 답하는 일이 생긴다. 판정 결과는 같아도 사용자는 서로 다른 지적을 받는다.
     *
     * @return 어긋난 조건, 규칙을 지켰으면 null
     */
    public static Violation validate(String password) {
        if (password == null || password.isEmpty()) {
            return new Violation("PASSWORD_TOO_SHORT", "비밀번호를 입력해 주세요");
        }

        // 문자 종류를 먼저 훑어 두고, 판정은 아래에서 순서대로 한다.
        // 훑는 도중에 바로 돌려주면 "사용 불가 문자"가 대문자 검사보다 앞서게 된다.
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        Character invalidChar = null;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                upper = true;
            } else if (c >= 'a' && c <= 'z') {
                lower = true;
            } else if (c >= '0' && c <= '9') {
                digit = true;
            } else if (SPECIALS.indexOf(c) >= 0) {
                special = true;
            } else if (invalidChar == null) {
                // 공백과 한글 등. 허용하면 나중에 본인도 못 치는 비밀번호가 생긴다.
                // 첫 글자만 기억한다 — 어차피 한 번에 하나만 알려준다.
                invalidChar = c;
            }
        }

        if (!upper) {
            return new Violation("PASSWORD_MISSING_UPPER", "비밀번호에 대문자가 필요합니다");
        }
        if (!lower) {
            return new Violation("PASSWORD_MISSING_LOWER", "비밀번호에 소문자가 필요합니다");
        }
        if (!digit) {
            return new Violation("PASSWORD_MISSING_DIGIT", "비밀번호에 숫자가 필요합니다");
        }
        if (!special) {
            return new Violation("PASSWORD_MISSING_SPECIAL", "비밀번호에 특수문자가 필요합니다");
        }
        if (invalidChar != null) {
            return new Violation("PASSWORD_INVALID_CHAR", "비밀번호에 쓸 수 없는 문자가 있습니다: '" + invalidChar + "'");
        }
        if (password.length() < MIN_LENGTH) {
            return new Violation("PASSWORD_TOO_SHORT", "비밀번호는 " + MIN_LENGTH + "자 이상이어야 합니다");
        }
        if (password.length() > MAX_LENGTH) {
            return new Violation("PASSWORD_TOO_LONG", "비밀번호는 " + MAX_LENGTH + "자 이하여야 합니다");
        }
        return null;
    }

    /** 규칙을 어기면 400 으로 나가도록 예외를 던진다. */
    public static void require(String password) {
        Violation violation = validate(password);
        if (violation != null) {
            throw new ValidationException(violation.errorCode(), violation.message());
        }
    }
}
