package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.chapchap.consumption.api.request.ConsumptionImageUploadRequest;
import kr.chapchap.consumption.api.response.ConsumptionImageUploadResponse;
import kr.chapchap.consumption.api.response.ConsumptionImageUrlResponse;
import kr.chapchap.consumption.application.service.ConsumptionImageQueryService;
import kr.chapchap.consumption.application.service.ConsumptionImageUploadService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.core.web.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consumption", description = "소비내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/consumptions/images")
public class ConsumptionImageController {

    private final ConsumptionImageUploadService uploadService;
    private final ConsumptionImageQueryService consumptionImageQueryService;

    @Operation(summary = "소비기록 이미지 업로드", description = """
            프론트에서 가공한 사진을 임시 저장합니다. JPEG·PNG, 최대 5MB·4096x4096을 지원합니다.
            반환한 imageId를 소비기록 등록 시 전달합니다. 24시간 내 연결하지 않은 이미지는 정리됩니다.
            잘못된 이미지·형식·해상도는 400, 용량 초과는 413, 저장소 연동 실패는 502를 반환합니다.
            """)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ConsumptionImageUploadResponse> upload(
            @ChapChapUserId Long userId,
            @Valid @ModelAttribute ConsumptionImageUploadRequest request
    ) {
        return ApiResponse.success(SuccessCode.CREATED,
                ConsumptionImageUploadResponse.from(uploadService.upload(request.toCommand(userId))));
    }

    @Operation(
            summary = "소비기록 이미지 조회 URL 재발급",
            description = """
                    소비기록 상세·장소 상세·방문 이력 등 다른 API가 내려주는 imageUrl은 S3 Presigned URL이라
                    발급 시점부터 10분간만 유효합니다. 화면에 오래 띄워두거나 캐시해둔 응답을 나중에 다시 쓰다가
                    만료되면(이미지 로드가 403으로 실패함), 그 응답에 같이 내려온 imageId를 그대로 이 API에 넣어
                    호출하면 같은 사진에 대한 새 10분짜리 URL을 다시 받을 수 있습니다.

                    - 업로드 직후 아직 소비기록에 연결하지 않은 임시 이미지(status=TEMPORARY)에도 쓸 수 있습니다.
                    - 다른 사용자의 imageId나 이미 정리(cleanup)되어 삭제된 imageId를 넣으면 404를 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 URL 재발급 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "본인 소유가 아니거나 존재하지 않는 imageId (CONSUMPTION021)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/{imageId}")
    public ApiResponse<ConsumptionImageUrlResponse> getReadUrl(
            @ChapChapUserId Long userId,
            @Parameter(description = "새 URL을 받을 이미지의 ID (다른 API 응답의 imageId를 그대로 전달)")
            @PathVariable Long imageId
    ) {
        String imageUrl = consumptionImageQueryService.getReadUrl(userId, imageId);
        return ApiResponse.success(new ConsumptionImageUrlResponse(imageId, imageUrl));
    }
}
