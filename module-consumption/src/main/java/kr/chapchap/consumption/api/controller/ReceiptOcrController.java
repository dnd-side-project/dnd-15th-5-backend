package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.chapchap.consumption.api.request.ReceiptOcrRequest;
import kr.chapchap.consumption.api.response.ReceiptOcrResponse;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;
import kr.chapchap.consumption.application.service.ReceiptOcrService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consumption", description = "소비내역 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/consumptions")
public class ReceiptOcrController {

    private final ReceiptOcrService receiptOcrService;

    @Operation(
            summary = "영수증 OCR 요청",
            description = """
                    영수증 이미지를 OCR 처리하고 사용자가 확인·수정할 인식 결과를 반환합니다.

                    인식하지 못한 항목은 null로 반환됩니다.
                    상호명이 인식되면 Google Places 검색 결과를 함께 반환하며,
                    장소 검색 또는 사진 조회에 실패해도 OCR 결과는 정상 반환됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OCR 처리 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "영수증 이미지가 유효하지 않음 (C001, CONSUMPTION004, CONSUMPTION006, CONSUMPTION007)",
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
                    responseCode = "413",
                    description = "영수증 이미지 크기 제한 초과 (C008, CONSUMPTION005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "영수증 인식 실패 (CONSUMPTION008)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "OCR 요청 한도 초과 (CONSUMPTION009)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "CLOVA OCR, 요청 제한 저장소 또는 이미지 저장소 연동 실패 (C007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping(value = "/receipt-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReceiptOcrResponse> recognizeReceipt(
            @ChapChapUserId Long userId,
            @Valid @ModelAttribute ReceiptOcrRequest request
    ) {
        ReceiptOcrInfo info = receiptOcrService.recognize(request.toCommand(userId));
        return ApiResponse.success(ReceiptOcrResponse.from(info));
    }
}
