package kr.chapchap.core.api.exception;

import kr.chapchap.core.api.response.ApiResponse;
import kr.chapchap.core.domain.exception.BusinessException;
import kr.chapchap.core.domain.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void BusinessException_발생_시_ErrorCode에_정의된_상태코드와_메시지로_응답한다() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT_VALUE);

        // when
        ResponseEntity<ApiResponse<Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getStatus());
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void BusinessException가_data를_담고_있으면_응답_body에도_data가_그대로_실린다() {
        // given
        Object data = Map.of("field", "name");
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT_VALUE, data);

        // when
        ResponseEntity<ApiResponse<Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getStatus());
        assertThat(response.getBody().data()).isEqualTo(data);
    }

    @Test
    void MethodArgumentNotValidException_발생_시_필드별_에러가_data에_담겨_400으로_응답한다() {
        // given
        FieldError fieldError = new FieldError("target", "name", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().data()).containsEntry("name", "must not be blank");
    }

    @Test
    void MissingServletRequestParameterException_발생_시_누락된_파라미터명이_data에_담긴다() {
        // given
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("page", "int");

        // when
        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleMissingServletRequestParameter(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().data()).containsKey("page");
    }

    @Test
    void AuthenticationException_발생_시_401로_응답한다() {
        // given
        BadCredentialsException exception = new BadCredentialsException("bad credentials");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthenticationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode());
    }

    @Test
    void AccessDeniedException_발생_시_403으로_응답한다() {
        // given
        AccessDeniedException exception = new AccessDeniedException("access denied");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.ACCESS_DENIED.getCode());
    }

    @Test
    void 예상하지_못한_예외_발생_시_500으로_응답한다() {
        // given
        RuntimeException exception = new RuntimeException("unexpected");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }
}
