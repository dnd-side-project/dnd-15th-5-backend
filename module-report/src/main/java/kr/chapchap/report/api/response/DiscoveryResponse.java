package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.DiscoveryInfo;

public record DiscoveryResponse(
        String message,
        int newStickerCount
) {

    public static DiscoveryResponse from(DiscoveryInfo info) {
        return new DiscoveryResponse(info.message(), info.newStickerCount());
    }
}
