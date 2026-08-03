package com.reviewticket.server.domain;

/** IP 시도 로그(AuthAttemptLog)에 남기는 행위 종류. */
public enum AuthAction {
    LOGIN,
    SIGNUP,
    PASSWORD_CHANGE,
    NICKNAME_CHANGE
}
