package kr.chapchap.place.infra.external.legaldong;


public record LegalDongCodeRow(
        String regionCd, //법정동코드 10자리
        String sidoCd,
        String sggCd,
        String umdCd,
        String riCd,
        String locataddNm,
        String locatRm
) {
    private static final String ABOLISHED_MARKER = "폐지";
    private static final String NONE_LEVEL = "000";

    public boolean isAbolished() {
        return locatRm != null && locatRm.contains(ABOLISHED_MARKER);
    }

    // 시/군/구와 읍/면/동이 모두 있는(법정리까지는 내려가지 않은) 행정동 단위 행만 우리 검색 대상으로 쓴다.
    public boolean isDongLevel() {
        return sggCd != null && !NONE_LEVEL.equals(sggCd)
                && umdCd != null && !NONE_LEVEL.equals(umdCd)
                && (riCd == null || riCd.isBlank() || "00".equals(riCd));
    }
}
