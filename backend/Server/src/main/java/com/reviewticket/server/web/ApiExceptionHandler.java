package com.reviewticket.server.web;

import java.util.LinkedHashMap;
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
import com.reviewticket.server.auth.TooManyRequestsException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.auth.ValidationException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * 응답에는 errorCode 만 담는다. 사용자에게 보여줄 문구는 화면이 그 코드를
     * 보고 채운다 — 문구를 서버가 정하면 어투 하나 고칠 때마다 화면의 분기가
     * 조용히 깨진다.
     *
     * 예외의 message 는 응답에 싣지 않고 서버 로그와 스택트레이스에만 남긴다.
     */
    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String errorCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", status.getReasonPhrase());
        payload.put("errorCode", errorCode);
        return ResponseEntity.status(status).body(payload);
    }

    @ExceptionHandler({ IllegalArgumentException.class, ConstraintViolationException.class })
    public ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        if (e instanceof ValidationException ve) {
            return body(HttpStatus.BAD_REQUEST, ve.getErrorCode());
        }
        // 코드를 붙이지 않은 곳에서 올라온 경우. 화면은 일반 안내로 처리한다.
        log.warn("errorCode 없는 400", e);
        return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
    }

    /**
     * @Valid 위반. 어느 필드가 왜 틀렸는지는 응답에 담지 않고 로그로만 남긴다 —
     * 이 경로는 화면이 이미 같은 규칙으로 걸러 낸 뒤라 정상 사용자는 닿지 않는다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
        String reason = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다");
        log.warn("요청 본문 검증 실패: {}", reason);
        return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
    }

    /** 이미 쓰이고 있는 이메일·닉네임. 형식 오류와 달리 다른 값을 쓰라고 안내해야 한다. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
        return body(HttpStatus.CONFLICT, e.getErrorCode());
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
        return body(HttpStatus.CONFLICT, "ALREADY_IN_USE");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, e.getErrorCode());
    }

    /**
     * 남은 차단 시간을 함께 보낸다. 화면이 그 시간을 세어 보여주고, 그동안
     * 로그인·가입 버튼을 막는다. Retry-After 헤더로도 같은 값을 보내지만
     * 본문에 넣는 이유는 — 프론트가 다른 출처에서 뜨는 경우 헤더를 읽으려면
     * CORS 노출 설정이 따로 필요한데, 본문은 그런 제약이 없다.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> tooManyRequests(TooManyRequestsException e) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        payload.put("errorCode", "TOO_MANY_REQUESTS");
        payload.put("retryAfterSeconds", e.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .body(payload);
    }

}
