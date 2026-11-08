package kr.chapchap.report.application.info;

import java.math.BigDecimal;
import java.util.List;

public record CategoryDistributionInfo(List<CategoryItemInfo> categories) {

    public record CategoryItemInfo(String category, long count, BigDecimal percentage) {
    }
}
