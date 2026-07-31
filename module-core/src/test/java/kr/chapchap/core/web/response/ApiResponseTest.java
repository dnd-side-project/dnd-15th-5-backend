package kr.chapchap.core.web.response;

import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void data와_함께_success_응답을_생성하면_SuccessCode_OK와_data가_담긴다() {
        // given
        String data = "response-data";

        // when
        ApiResponse<String> response = ApiResponse.success(data);

        // then
        assertThat(response.code()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(response.message()).isEqualTo(SuccessCode.OK.getMessage());
        assertThat(response.data()).isEqualTo(data);
    }

    @Test
    void ok_호출_시_data가_없는_성공_응답이_생성된다() {
        // when
        ApiResponse<Void> response = ApiResponse.ok();

        // then
        assertThat(response.code()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(response.data()).isNull();
    }

    @Test
    void fail_호출_시_ErrorCode의_code와_message가_그대로_담긴다() {
        // when
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE);

        // then
        assertThat(response.code()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.message()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        assertThat(response.data()).isNull();
    }

    @Test
    void fail_호출_시_커스텀_메시지로_ErrorCode_기본_메시지를_덮어쓴다() {
        // given
        String customMessage = "커스텀 에러 메시지";

        // when
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, customMessage);

        // then
        assertThat(response.code()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.message()).isEqualTo(customMessage);
    }

    @Test
    void failWithData_호출_시_ErrorCode_기본_메시지와_data가_함께_담긴다() {
        // given
        Map<String, String> errors = Map.of("field", "must not be blank");

        // when
        ApiResponse<Map<String, String>> response =
                ApiResponse.failWithData(ErrorCode.INVALID_INPUT_VALUE, errors);

        // then
        assertThat(response.code()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.message()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        assertThat(response.data()).isEqualTo(errors);
    }
}
