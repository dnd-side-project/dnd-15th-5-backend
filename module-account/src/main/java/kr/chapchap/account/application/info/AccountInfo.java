package kr.chapchap.account.application.info;

import kr.chapchap.account.domain.entity.User;

public record AccountInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {

    public static AccountInfo from(
            User user,
            String profileImageUrl
    ) {
        return new AccountInfo(
                user.getId(),
                user.getNickname(),
                profileImageUrl
        );
    }
}
