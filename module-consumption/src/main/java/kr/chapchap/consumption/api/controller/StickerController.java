package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.consumption.api.response.StickerItemResponse;
import kr.chapchap.consumption.application.service.StickerQueryService;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consumption", description = "소비내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/consumptions/stickers")
public class StickerController {
    private final StickerQueryService stickerQueryService;

    @Operation(summary = "등록용 스티커 목록 조회")
    @GetMapping
    public ApiResponse<List<StickerItemResponse>> findStickers() {
        return ApiResponse.success(stickerQueryService.findAll().stream().map(StickerItemResponse::from).toList());
    }
}
