package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlaceLocationInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.port.PlaceLocationLookupPort;
import kr.chapchap.consumption.application.port.PlaceNameLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class VisitedPlaceQueryService {

    private static final String UNKNOWN_PLACE_NAME = "알 수 없는 가게";

    private final ConsumptionQueryRepository consumptionQueryRepository; //소비 집계
    private final PlaceNameLookupPort placeNameLookupPort; //가게 이름 조회
    private final PlaceLocationLookupPort placeLocationLookupPort; //위치 정보 조회

    public List<VisitedPlaceMarkerInfo> getVisitedPlaceMarkers(Long userId, List<String> categories) {
        //place id , 카테고리 , 횟수
        List<PlaceCategoryVisitRow> rows = consumptionQueryRepository.aggregateVisitedPlacesByCategory(userId, categories);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> placeIds = rows.stream().map(PlaceCategoryVisitRow::placeId).toList();
        Map<Long, String> placeNames = placeNameLookupPort.findNames(placeIds);
        Map<Long, PlaceLocationInfo> placeLocations = placeLocationLookupPort.findLocations(placeIds);

        return rows.stream()
                .map(row -> toMarkerInfo(row, placeNames, placeLocations))
                .sorted(Comparator.comparing(VisitedPlaceMarkerInfo::visitCount).reversed())
                .toList();
    }

    private VisitedPlaceMarkerInfo toMarkerInfo(PlaceCategoryVisitRow row, Map<Long, String> placeNames, Map<Long, PlaceLocationInfo> placeLocations) {
        PlaceLocationInfo location = placeLocations.get(row.placeId());

        return new VisitedPlaceMarkerInfo(
                row.placeId(),
                placeNames.getOrDefault(row.placeId(), UNKNOWN_PLACE_NAME),
                row.category(),
                location.latitude(),
                location.longitude(),
                row.visitCount()
        );
    }
}
