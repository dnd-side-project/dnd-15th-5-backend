package kr.chapchap.place.application.info;

import kr.chapchap.place.domain.entity.UserTown;

import java.time.LocalDateTime;

public record UserTownInfo(
        Long id,
        String administrativeDongCode,
        String administrativeDongName,
        double latitude,
        double longitude,
        LocalDateTime selectedAt
) {
    public static UserTownInfo from(UserTown userTown) {
        return new UserTownInfo(
                userTown.getId(),
                userTown.getAdministrativeDongCode(),
                userTown.getAdministrativeDongName(),
                userTown.getLatitude(),
                userTown.getLongitude(),
                userTown.getSelectedAt()
        );
    }
}
