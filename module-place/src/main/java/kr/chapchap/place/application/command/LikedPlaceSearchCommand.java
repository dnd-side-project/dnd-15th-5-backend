package kr.chapchap.place.application.command;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;

public record LikedPlaceSearchCommand(Long userId, String category, Long cursorId, int size) {

    private static final int MAX_SIZE = 5;
    private static final int MAX_CATEGORY_LENGTH = 40;

    public LikedPlaceSearchCommand {
        category = category == null || category.isBlank() ? null : category.strip();
        if (size < 1 || size > MAX_SIZE
                || (cursorId != null && cursorId <= 0)
                || (category != null && category.length() > MAX_CATEGORY_LENGTH)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
