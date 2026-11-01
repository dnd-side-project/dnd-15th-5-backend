package kr.chapchap.place.api.response;

import kr.chapchap.place.application.info.UserTownInfo;

import java.time.LocalDateTime;

public record UserTownResponse(
        Long id,
        String administrativeDongCode,
        String administrativeDongName,
        double latitude,
        double longitude,
        LocalDateTime selectedAt
) {
    public static UserTownResponse from(UserTownInfo info) {
        return new UserTownResponse(
                info.id(), info.administrativeDongCode(), info.administrativeDongName(),
                info.latitude(), info.longitude(), info.selectedAt());
    }
}
