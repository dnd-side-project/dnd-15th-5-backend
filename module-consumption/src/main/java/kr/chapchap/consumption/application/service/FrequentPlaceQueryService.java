package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.FrequentPlaceRankCommand;
import kr.chapchap.consumption.application.command.RankingPeriod;
import kr.chapchap.consumption.application.info.FrequentPlaceRankInfo;
import kr.chapchap.consumption.application.info.FrequentPlaceRankInfo.PlaceRankInfo;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class FrequentPlaceQueryService {

    private static final PlaceSummaryInfo UNKNOWN_PLACE_SUMMARY =
            new PlaceSummaryInfo("알 수 없는 가게", "알 수 없는 지역", null, null, null);

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final PlaceSummaryLookupPort placeSummaryLookupPort;
    private final PlaceThumbnailBatchLookup placeThumbnailBatchLookup;
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
        Map<Long, PlaceSummaryInfo> summaries = placeSummaryLookupPort.findSummaries(placeIds);
        Map<Long, PlacePhotoInfo> thumbnails = findThumbnails(placeIds, summaries);

        List<PlaceRankInfo> places = IntStream.range(0, content.size())
                .mapToObj(i -> {
                    PlaceCategoryVisitRow row = content.get(i);
                    PlaceSummaryInfo summary = summaries.getOrDefault(row.placeId(), UNKNOWN_PLACE_SUMMARY);
                    PlacePhotoInfo photo = thumbnails.get(row.placeId());
                    return new PlaceRankInfo(
                            command.cursorRank() + i + 1,
                            row.placeId(),
                            summary.name(),
                            row.category(),
                            summary.dongName(),
                            row.visitCount(),
                            photo != null ? photo.thumbnailUrl() : null
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

    private Map<Long, PlacePhotoInfo> findThumbnails(List<Long> placeIds, Map<Long, PlaceSummaryInfo> summaries) {
        Map<Long, String> googlePlaceIdsByPlaceId = new LinkedHashMap<>();
        for (Long placeId : placeIds) {
            PlaceSummaryInfo summary = summaries.get(placeId);
            String googlePlaceId = summary != null ? summary.googlePlaceId() : null;
            if (googlePlaceId != null && !googlePlaceId.isBlank()) {
                googlePlaceIdsByPlaceId.put(placeId, googlePlaceId);
            }
        }
        return placeThumbnailBatchLookup.findThumbnails(googlePlaceIdsByPlaceId);
    }
}
