package kr.chopchop.core.api.exception;

import kr.chopchop.core.api.response.ApiResponse;
import kr.chopchop.core.domain.exception.BusinessException;
import kr.chopchop.core.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{}] BusinessException: {}", errorCode.getCode(), e.getMessage(), e);
        ApiResponse<Object> body = e.getData() != null
                ? ApiResponse.failWithData(errorCode, e.getData())
                : ApiResponse.fail(errorCode, e.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // @Valid 검증 실패시
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>(); // 입력 순서 보존용 LinkedHashMap
        e.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        log.warn("Validation 실패: {}", errors);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.failWithData(ErrorCode.INVALID_INPUT_VALUE, errors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {
        Map<String, String> errors = Map.of(e.getParameterName(), "필수 요청 파라미터입니다.");
        log.warn("필수 요청 파라미터 누락: {}", errors);
        return ResponseEntity.status(ErrorCode.MISSING_REQUIRED_FIELD.getStatus())
                .body(ApiResponse.failWithData(ErrorCode.MISSING_REQUIRED_FIELD, errors));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.AUTHENTICATION_REQUIRED.getStatus())
                .body(ApiResponse.fail(ErrorCode.AUTHENTICATION_REQUIRED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ApiResponse.fail(ErrorCode.ACCESS_DENIED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception occurred", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
