package kr.chapchap.place.application.command;

public record UserTownSaveCommand(
        Long userId,
        String administrativeDongCode,
        String administrativeDongName,
        double latitude,
        double longitude
) {
}
