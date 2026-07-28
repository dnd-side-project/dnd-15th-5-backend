package com.dnd.core.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void ErrorCode만으로_생성하면_메시지는_ErrorCode의_기본_메시지를_따르고_data는_null이다() {
        // when
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT_VALUE);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        assertThat(exception.getData()).isNull();
    }

    @Test
    void data와_함께_생성하면_data가_그대로_보존된다() {
        // given
        Object data = "invalid-field";

        // when
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT_VALUE, data);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception.getData()).isEqualTo(data);
    }
}