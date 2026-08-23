package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

public record VisitedPlaceSearchCommand(
        Long userId,
        String keyword,
        String cursor,
        int size
) {

    private static final int MAX_SIZE = 5;
    private static final int MAX_KEYWORD_LENGTH = 100;

    public VisitedPlaceSearchCommand {
        String normalizedKeyword = keyword == null ? null : keyword.strip();
        if (normalizedKeyword == null
                || normalizedKeyword.isEmpty()
                || normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_SIZE);
        }

        keyword = normalizedKeyword;
        cursor = cursor == null || cursor.isBlank() ? null : cursor.trim();
    }
}
