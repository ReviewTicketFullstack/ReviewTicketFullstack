package com.reviewticket.server.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.UnauthorizedException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, boolean retryable) {
        return ResponseEntity.status(status).body(Map.of(
                "error", status.getReasonPhrase(),
                "message", message,
                "retryable", retryable));
    }

    @ExceptionHandler({ IllegalArgumentException.class, ConstraintViolationException.class })
    public ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), false);
    }

    /** @Valid 가 걸린 요청 본문의 첫 위반 사유를 그대로 보여준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다");
        return body(HttpStatus.BAD_REQUEST, message, false);
    }

    /** 이미 쓰이고 있는 이메일·닉네임. 형식 오류와 달리 다른 값을 쓰라고 안내해야 한다. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
        return body(HttpStatus.CONFLICT, e.getMessage(), false);
    }

    /**
     * DB 제약 위반. 실질적으로는 동시 가입 경합이다 — 두 요청이 중복 검사를
     * 각자 통과한 뒤 저장 단계에서 UNIQUE 에 걸리는 경우다.
     *
     * 검사와 저장 사이의 틈은 애플리케이션 코드로 없앨 수 없다. 최종 방어선은
     * DB 제약이고, 여기서 그 결과를 사용자가 읽을 수 있는 409 로 바꿔준다.
     * 이 처리가 없으면 500 이 나가 "서버가 고장났다"로 보인다.
     *
     * 어느 제약에 걸렸는지는 알려주지 않는다 — 예외 메시지에 SQL 과 표·컬럼
     * 이름이 들어 있어 그대로 내보내면 내부 구조가 노출된다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> constraintViolation(DataIntegrityViolationException e) {
        log.warn("DB 제약 위반 (동시 요청 경합으로 추정)", e);
        return body(HttpStatus.CONFLICT,
                "이미 사용 중인 정보입니다. 다시 시도해 주세요.", true);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage(), false);
    }

}
