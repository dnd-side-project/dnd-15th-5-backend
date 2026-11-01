package kr.chapchap.place.api.response;

import kr.chapchap.place.application.info.TownSearchResultInfo;

import java.util.List;

public record TownSearchResponse(
        List<TownSearchItem> towns
) {
    public static TownSearchResponse from(List<TownSearchResultInfo> infos) {
        return new TownSearchResponse(infos.stream().map(TownSearchItem::from).toList());
    }

    public record TownSearchItem(
            String administrativeDongCode,
            String sidoName,
            String sigunguName,
            String dongName,
            String fullName
    ) {
        public static TownSearchItem from(TownSearchResultInfo info) {
            return new TownSearchItem(
                    info.administrativeDongCode(), info.sidoName(), info.sigunguName(),
                    info.dongName(), info.fullName());
        }
    }
}
