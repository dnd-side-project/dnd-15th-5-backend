package kr.chapchap.report.application.info;

import java.math.BigDecimal;

public record CategoryStatInfo(
        String category,
        BigDecimal percentage
) {
}
