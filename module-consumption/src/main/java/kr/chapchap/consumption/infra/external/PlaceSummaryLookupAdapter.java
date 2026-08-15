package kr.chapchap.consumption.infra.external;

import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.place.application.service.PlaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Component
public class PlaceSummaryLookupAdapter implements PlaceSummaryLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Map<Long, PlaceSummaryInfo> findSummaries(List<Long> placeIds) {
        return placeQueryService.findSummariesByIds(placeIds).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new PlaceSummaryInfo(
                                entry.getValue().name(), entry.getValue().dongName(), entry.getValue().address(),
                                entry.getValue().latitude(), entry.getValue().longitude())
                ));
    }
}
