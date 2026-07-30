package com.reviewticket.server.web;

import java.io.UncheckedIOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.reviewticket.server.ai.AiUnavailableException;
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

    /**
     * 추론 서버 장애. 절대 '거부'로 내려보내지 않는다 — 우리 서버 잘못으로
     * 정상 사용자가 티켓을 잃게 된다. 재시도 가능한 503 으로 알린다.
     */
    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, Object>> aiDown(AiUnavailableException e) {
        log.error("추론 서버 호출 실패", e);
        return body(HttpStatus.SERVICE_UNAVAILABLE,
                "판정 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.", true);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "사진이 너무 큽니다 (최대 15MB)", false);
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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage(), false);
    }

    /** 디코딩 실패 등. 사진이 깨졌거나 이미지가 아닌 경우다. */
    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, Object>> imageBroken(UncheckedIOException e) {
        log.warn("이미지 처리 실패", e);
        return body(HttpStatus.BAD_REQUEST, "사진을 읽을 수 없습니다", false);
    }
}
