package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.chapchap.consumption.api.request.ConsumptionImageUploadRequest;
import kr.chapchap.consumption.api.response.ConsumptionImageUploadResponse;
import kr.chapchap.consumption.application.service.ConsumptionImageUploadService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.core.web.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
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
}
