package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.CategoryStatInfo;

import java.math.BigDecimal;

public record CategoryStatResponse(
        String category,
        BigDecimal percentage
) {

    public static CategoryStatResponse from(CategoryStatInfo info) {
        return new CategoryStatResponse(info.category(), info.percentage());
    }
}
