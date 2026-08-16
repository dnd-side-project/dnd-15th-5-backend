package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.chapchap.consumption.api.request.ConsumptionCreateRequest;
import kr.chapchap.consumption.api.response.ConsumptionCreateResponse;
import kr.chapchap.consumption.application.info.ConsumptionInfo;
import kr.chapchap.consumption.application.service.ConsumptionCreateService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.core.web.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consumption", description = "소비내역 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/consumptions")
public class ConsumptionCreateController {

    private final ConsumptionCreateService consumptionCreateService;

    @Operation(
            summary = "소비 기록 등록",
            description = """
                    사용자가 확인한 장소와 소비 정보를 최종 저장합니다.
                    Google Place ID가 이미 등록된 장소라면 기존 장소를 재사용합니다.
                    신규 장소라면 도로명주소를 SGIS로 조회해 행정동을 저장합니다.
                    receiptImageId는 영수증 OCR을 사용한 경우에만 전달합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "소비 기록 등록 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값이 유효하지 않음 (C001, CONSUMPTION010)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한이 없음 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용 가능한 영수증 이미지를 찾을 수 없음 (CONSUMPTION011)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "영수증 이미지가 이미 사용됐거나 만료됨 (CONSUMPTION012, CONSUMPTION013)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "도로명주소의 행정동을 찾을 수 없음 (PLACE003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "SGIS 연동 실패 (C007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<ConsumptionCreateResponse> createConsumption(
            @ChapChapUserId Long userId,
            @Valid @RequestBody ConsumptionCreateRequest request
    ) {
        ConsumptionInfo info = consumptionCreateService.create(request.toCommand(userId));
        return ApiResponse.success(SuccessCode.CREATED, ConsumptionCreateResponse.from(info));
    }
}
