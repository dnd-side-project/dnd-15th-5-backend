package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.FrequentPlaceRankCommand;
import kr.chapchap.consumption.application.command.RankingPeriod;
import kr.chapchap.consumption.application.info.FrequentPlaceInfo;
import kr.chapchap.consumption.application.info.FrequentPlaceRankInfo;
import kr.chapchap.consumption.application.port.PlaceDongNameLookupPort;
import kr.chapchap.consumption.application.port.PlaceNameLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class FrequentPlaceQueryService {

    private static final String UNKNOWN_PLACE_NAME = "알 수 없는 가게";
    private static final String UNKNOWN_DONG_NAME = "알 수 없는 지역";

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final PlaceNameLookupPort placeNameLookupPort;
    private final PlaceDongNameLookupPort placeDongNameLookupPort;
    private final Clock clock;

    public FrequentPlaceRankInfo getFrequentPlaces(FrequentPlaceRankCommand command) {
        LocalDate from = null;
        LocalDate toExclusive = null;

        if (command.period() == RankingPeriod.THIS_MONTH) {
            YearMonth currentMonth = YearMonth.now(clock);
            from = currentMonth.atDay(1);
            toExclusive = currentMonth.plusMonths(1).atDay(1);
        }

        int fetchSize = command.size() + 1;

        List<PlaceCategoryVisitRow> fetched = consumptionQueryRepository.aggregatePlaceRankingByCursor(
                command.userId(), from, toExclusive, command.categories(),
                command.cursorVisitCount(), command.cursorPlaceId(), fetchSize
        );

        boolean hasNext = fetched.size() > command.size();
        List<PlaceCategoryVisitRow> content = hasNext ? fetched.subList(0, command.size()) : fetched;

        List<Long> placeIds = content.stream().map(PlaceCategoryVisitRow::placeId).toList();
        Map<Long, String> placeNames = placeNameLookupPort.findNames(placeIds);
        Map<Long, String> dongNames = placeDongNameLookupPort.findDongNames(placeIds);

        List<FrequentPlaceInfo> places = IntStream.range(0, content.size())
                .mapToObj(i -> {
                    PlaceCategoryVisitRow row = content.get(i);
                    return new FrequentPlaceInfo(
                            command.cursorRank() + i + 1,
                            row.placeId(),
                            placeNames.getOrDefault(row.placeId(), UNKNOWN_PLACE_NAME),
                            row.category(),
                            dongNames.getOrDefault(row.placeId(), UNKNOWN_DONG_NAME),
                            row.visitCount()
                    );
                })
                .toList();

        PlaceCategoryVisitRow last = content.isEmpty() ? null : content.get(content.size() - 1);

        return new FrequentPlaceRankInfo(
                places,
                hasNext,
                last != null ? last.visitCount() : null,
                last != null ? last.placeId() : null,
                places.isEmpty() ? command.cursorRank() : places.get(places.size() - 1).rank()
        );
    }
}
