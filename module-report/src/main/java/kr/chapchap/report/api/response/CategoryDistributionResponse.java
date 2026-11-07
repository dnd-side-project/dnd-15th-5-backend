package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.CategoryDistributionInfo;
import kr.chapchap.report.application.info.CategoryDistributionInfo.CategoryItemInfo;

import java.math.BigDecimal;
import java.util.List;

public record CategoryDistributionResponse(List<CategoryItemResponse> categories) {

    public static CategoryDistributionResponse from(CategoryDistributionInfo info) {
        return new CategoryDistributionResponse(info.categories().stream().map(CategoryItemResponse::from).toList());
    }

    public record CategoryItemResponse(String category, long count, BigDecimal percentage) {

        public static CategoryItemResponse from(CategoryItemInfo info) {
            return new CategoryItemResponse(info.category(), info.count(), info.percentage());
        }
    }
}
