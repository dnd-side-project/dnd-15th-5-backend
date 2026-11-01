package kr.chapchap.place.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.chapchap.place.application.command.UserTownSaveCommand;

@Schema(description = "동네 설정하기 요청. 동네 검색 결과를 선택한 경우 administrativeDongCode를 같이 보내고, "
        + "지도에서 핀을 찍어 저장한 정밀 위치(도로명 주소 등)는 우리 쪽 행정동코드를 알 수 없으므로 생략한다.")
public record UserTownSaveRequest(
        @Schema(description = "행정동 코드. 동네 검색 결과를 선택한 경우에만 보낸다 - 지도 핀 찍기 저장은 생략 가능",
                example = "SAMPLE00001", nullable = true)
        @Size(max = 20, message = "행정동 코드는 20자 이하여야 합니다.")
        String administrativeDongCode,

        @Schema(description = "최근 설정한 동네 목록에 보여줄 이름. 행정동 이름일 수도, 도로명 주소일 수도 있다",
                example = "서울특별시 성동구 성수1가동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String administrativeDongName,

        @Schema(description = "프론트에서 변환한 위도", example = "37.544")
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @Schema(description = "프론트에서 변환한 경도", example = "127.055")
        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        Double longitude
) {
    public UserTownSaveCommand toCommand(Long userId) {
        return new UserTownSaveCommand(userId, administrativeDongCode, administrativeDongName, latitude, longitude);
    }
}
