package kr.chapchap.place.application.info;

import kr.chapchap.place.domain.entity.AdministrativeDong;

public record TownSearchResultInfo(
        String administrativeDongCode,
        String sidoName,
        String sigunguName,
        String dongName,
        String fullName
) {
    public static TownSearchResultInfo from(AdministrativeDong administrativeDong) {
        return new TownSearchResultInfo(
                administrativeDong.getCode(),
                administrativeDong.getSidoName(),
                administrativeDong.getSigunguName(),
                administrativeDong.getDongName(),
                administrativeDong.getFullName()
        );
    }
}
