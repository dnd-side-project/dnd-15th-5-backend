package kr.chapchap.consumption.application.command;

import java.util.List;

public record FrequentPlaceRankCommand(
        Long userId,
        RankingPeriod period,
        List<String> categories,
        Long cursorVisitCount,
        Long cursorPlaceId,
        int cursorRank,
        int size
) { }
