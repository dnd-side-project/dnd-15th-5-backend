package kr.chapchap.consumption.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "소비 기록 등록 요청")
public record ConsumptionCreateRequest(
        @Schema(description = "OCR에서 발급받은 임시 영수증 이미지 ID", example = "15", nullable = true)
        @Positive(message = "영수증 이미지 ID는 0보다 커야 합니다.")
        Long receiptImageId,

        @Schema(description = "Google Place ID", example = "ChIJxxxxxxxxxxxxxxxx")
        @NotBlank(message = "Google Place ID는 필수입니다.")
        @Size(max = 255, message = "Google Place ID는 255자 이하여야 합니다.")
        String googlePlaceId,

        @Schema(description = "장소명", example = "투썸플레이스 신논현점")
        @NotBlank(message = "장소명은 필수입니다.")
        @Size(max = 100, message = "장소명은 100자 이하여야 합니다.")
        String placeName,

        @Schema(description = "사용자가 확인한 도로명주소", example = "서울특별시 강남구 봉은사로 125 1층")
        @NotBlank(message = "도로명주소는 필수입니다.")
        @Size(max = 255, message = "도로명주소는 255자 이하여야 합니다.")
        String roadAddress,

        @Schema(description = "Google Places에서 받은 위도", example = "37.506481")
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @Schema(description = "Google Places에서 받은 경도", example = "127.024551")
        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        Double longitude,

        @Schema(description = "소비 날짜", example = "2026-07-25")
        @NotNull(message = "소비 날짜는 필수입니다.")
        LocalDate purchaseDate,

        @Schema(description = "소비 시간", example = "11:20:00")
        @NotNull(message = "소비 시간은 필수입니다.")
        LocalTime purchaseTime,

        @Schema(description = "소비 금액(원)", example = "33000")
        @NotNull(message = "소비 금액은 필수입니다.")
        @Positive(message = "소비 금액은 0보다 커야 합니다.")
        Long amount,

        @Schema(description = "소비 카테고리", example = "카페")
        @NotBlank(message = "카테고리는 필수입니다.")
        @Size(max = 40, message = "카테고리는 40자 이하여야 합니다.")
        @Pattern(
                regexp = "^(카페|운동|편의점/마트|취미/놀거리|음식점|미용/뷰티|기타)$",
                message = "지원하지 않는 카테고리입니다."
        )
        String category
) {

    public ConsumptionCreateRequest {
        googlePlaceId = trim(googlePlaceId);
        placeName = trim(placeName);
        roadAddress = trim(roadAddress);
        category = trim(category);
    }

    public ConsumptionCreateCommand toCommand(Long userId) {
        PlaceResolveCommand place = new PlaceResolveCommand(
                googlePlaceId,
                placeName,
                roadAddress,
                latitude,
                longitude
        );
        return new ConsumptionCreateCommand(
                userId,
                receiptImageId,
                place,
                purchaseDate,
                purchaseTime,
                amount,
                category
        );
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
