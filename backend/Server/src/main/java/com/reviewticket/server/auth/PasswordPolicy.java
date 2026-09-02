package com.reviewticket.server.auth;

/**
 * 비밀번호 형식 규칙. 대문자, 소문자, 숫자, 특수문자를 모두 포함하고 8자 이상.
 * 예: Abc123!@
 *
 * 프론트에서도 같은 규칙으로 즉시 검사하지만, 여기서 다시 본다.
 * 프론트 검증은 사용자 편의를 위한 것이고 우회할 수 있다 — 규칙을
 * 실제로 강제하는 곳은 서버뿐이다.
 *
 * 정규식 하나로 몰아넣지 않고 조건별로 나눈 이유: 어떤 조건이 빠졌는지
 * 사용자에게 알려줄 수 있다. "형식이 틀렸습니다"만 띄우면 뭘 고쳐야
 * 할지 몰라 같은 실수를 반복한다.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72; // BCrypt 가 72바이트를 넘으면 조용히 잘라낸다

    /** 키보드로 칠 수 있는 ASCII 특수문자 전부. */
    private static final String SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private PasswordPolicy() {
    }

    /**
     * @return 어긋난 조건을 설명하는 문장, 규칙을 지켰으면 null
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "비밀번호를 입력해 주세요";
        }
        if (password.length() < MIN_LENGTH) {
            return "비밀번호는 " + MIN_LENGTH + "자 이상이어야 합니다";
        }
        if (password.length() > MAX_LENGTH) {
            return "비밀번호는 " + MAX_LENGTH + "자 이하여야 합니다";
        }

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
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
            } else {
                // 공백과 한글 등. 허용하면 나중에 본인도 못 치는 비밀번호가 생긴다.
                return "비밀번호에 쓸 수 없는 문자가 있습니다: '" + c + "'";
            }
        }

        if (!upper) {
            return "비밀번호에 대문자가 필요합니다";
        }
        if (!lower) {
            return "비밀번호에 소문자가 필요합니다";
        }
        if (!digit) {
            return "비밀번호에 숫자가 필요합니다";
        }
        if (!special) {
            return "비밀번호에 특수문자가 필요합니다";
        }
        return null;
    }

    /** 규칙을 어기면 400 으로 나가도록 예외를 던진다. */
    public static void require(String password) {
        String problem = validate(password);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
    }
}
