package kr.chapchap.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void ErrorCode만으로_생성하면_메시지는_ErrorCode의_기본_메시지를_따르고_data는_null이다() {
        // when
        BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception.getMessage()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.getMessage());
        assertThat(exception.getData()).isNull();
    }

    @Test
    void data와_함께_생성하면_data가_그대로_보존된다() {
        // given
        Object data = "invalid-field";

        // when
        BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, data);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception.getData()).isEqualTo(data);
    }

    @Test
    void cause와_함께_생성하면_원본_예외가_보존된다() {
        // given
        RuntimeException cause = new RuntimeException("외부 연동 실패");

        // when
        BusinessException exception = new BusinessException(
                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                cause
        );

        // then
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getData()).isNull();
    }
}
