package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.StickerItemInfo;

public record StickerItemResponse(
        @Schema(description = "소비기록 등록 시 전달할 스티커 ID") Long stickerItemId,
        @Schema(description = "스티커 선택 시 함께 지정할 소비 카테고리") String category,
        @Schema(description = "스티커 이름") String name
) {
    public static StickerItemResponse from(StickerItemInfo info) {
        return new StickerItemResponse(info.stickerItemId(), info.category(), info.name());
    }
}
