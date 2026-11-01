package kr.chapchap.account.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "알림 수신 설정 변경 요청. 미전달 또는 null인 항목은 유지합니다.")
public record NotificationSettingsUpdateRequest(
        @Schema(description = "소비 기록 푸시 알림 수신 여부", example = "false")
        Boolean consumptionReminderEnabled,

        @Schema(description = "월간 리포트 푸시 알림 수신 여부", example = "true")
        Boolean monthlyReportEnabled
) {
    @JsonIgnore
    @AssertTrue(message = "수정할 알림 설정을 하나 이상 입력해야 합니다.")
    public boolean isUpdateRequested() {
        return consumptionReminderEnabled != null || monthlyReportEnabled != null;
    }
}
