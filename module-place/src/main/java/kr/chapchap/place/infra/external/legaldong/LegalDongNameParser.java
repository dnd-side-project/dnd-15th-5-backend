package kr.chapchap.place.infra.external.legaldong;


final class LegalDongNameParser {

    private LegalDongNameParser() {
    }

    record ParsedName(String sidoName, String sigunguName, String dongName) {
    }

    static ParsedName parse(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String[] tokens = fullName.trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        if (tokens.length == 2) {
            return new ParsedName(tokens[0], "", tokens[1]);
        }
        String sido = tokens[0];
        String dong = tokens[tokens.length - 1];
        String sigungu = String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, tokens.length - 1));
        return new ParsedName(sido, sigungu, dong);
    }
}
