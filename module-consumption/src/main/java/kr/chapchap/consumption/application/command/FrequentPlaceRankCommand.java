package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

import java.util.List;

public record FrequentPlaceRankCommand(
        Long userId,
        RankingPeriod period,
        List<String> categories,
        Long cursorVisitCount,
        Long cursorPlaceId,
        int cursorRank,
        int size
) {
    public FrequentPlaceRankCommand {
        if (size < 1) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_SIZE);
        }
    }
}
