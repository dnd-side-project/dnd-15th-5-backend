package kr.chapchap.account.application.command;

public record AccountUpdateCommand(
        Long userId,
        String nickname,
        byte[] profileImageContent,
        boolean deleteProfileImage
) {

    public boolean hasProfileImage() {
        return profileImageContent != null;
    }
}
