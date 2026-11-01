package kr.chapchap.place.api.response;

import kr.chapchap.place.application.info.UserTownInfo;

import java.util.List;

public record RecentTownsResponse(
        List<UserTownResponse> towns
) {
    public static RecentTownsResponse from(List<UserTownInfo> infos) {
        return new RecentTownsResponse(infos.stream().map(UserTownResponse::from).toList());
    }
}
