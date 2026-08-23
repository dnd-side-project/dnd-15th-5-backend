package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.VisitedPlaceSearchCommand;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo.VisitedPlaceInfo;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.service.PlacePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class VisitedPlaceSearchService {

    private static final int SCAN_BATCH_SIZE = 500;
    private static final int CURSOR_PART_COUNT = 4;
    private static final int MAX_CURSOR_LENGTH = 256;
    private static final String CURSOR_VERSION = "v1";
    private static final String CURSOR_DELIMITER = "|";

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final PlaceSummaryLookupPort placeSummaryLookupPort;
    private final PlacePhotoService placePhotoService;

    public VisitedPlaceSearchInfo search(VisitedPlaceSearchCommand command) {
        VisitCursor scanCursor = decodeCursor(command.cursor());
        String normalizedKeyword = command.keyword().toLowerCase(Locale.ROOT);
        List<MatchedPlace> matchedPlaces = new ArrayList<>(command.size() + 1);

        while (matchedPlaces.size() <= command.size()) {
            List<Consumption> latestVisits = consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                    command.userId(),
                    scanCursor != null ? scanCursor.purchaseDate() : null,
                    scanCursor != null ? scanCursor.purchaseTime() : null,
                    scanCursor != null ? scanCursor.consumptionId() : null,
                    SCAN_BATCH_SIZE
            );
            if (latestVisits.isEmpty()) {
                break;
            }

            Map<Long, PlaceSummaryInfo> summaries = placeSummaryLookupPort.findSummaries(
                    latestVisits.stream().map(Consumption::getPlaceId).toList()
            );
            for (Consumption latestVisit : latestVisits) {
                PlaceSummaryInfo summary = summaries.get(latestVisit.getPlaceId());
                if (summary != null && matches(summary, normalizedKeyword)) {
                    matchedPlaces.add(new MatchedPlace(latestVisit, summary));
                    if (matchedPlaces.size() > command.size()) {
                        break;
                    }
                }
            }
            if (matchedPlaces.size() > command.size()) {
                break;
            }

            Consumption lastScanned = latestVisits.get(latestVisits.size() - 1);
            scanCursor = VisitCursor.from(lastScanned);
            if (latestVisits.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }

        boolean hasNext = matchedPlaces.size() > command.size();
        List<MatchedPlace> content = hasNext
                ? matchedPlaces.subList(0, command.size())
                : matchedPlaces;
        Map<Long, PlacePhotoInfo> photos = findPhotos(content);
        List<VisitedPlaceInfo> places = content.stream()
                .map(place -> toInfo(place, photos.get(place.latestVisit().getPlaceId())))
                .toList();
        String nextCursor = hasNext
                ? encodeCursor(VisitCursor.from(content.get(content.size() - 1).latestVisit()))
                : null;

        return new VisitedPlaceSearchInfo(places, hasNext, nextCursor);
    }

    private Map<Long, PlacePhotoInfo> findPhotos(List<MatchedPlace> places) {
        Map<Long, String> googlePlaceIdsByPlaceId = new LinkedHashMap<>();
        for (MatchedPlace place : places) {
            String googlePlaceId = place.summary().googlePlaceId();
            if (googlePlaceId != null && !googlePlaceId.isBlank()) {
                googlePlaceIdsByPlaceId.put(place.latestVisit().getPlaceId(), googlePlaceId);
            }
        }
        return googlePlaceIdsByPlaceId.isEmpty()
                ? Map.of()
                : placePhotoService.findThumbnails(googlePlaceIdsByPlaceId);
    }

    private VisitedPlaceInfo toInfo(MatchedPlace place, PlacePhotoInfo photo) {
        return new VisitedPlaceInfo(
                place.latestVisit().getPlaceId(),
                place.summary().name(),
                place.summary().address(),
                photo != null ? photo.thumbnailUrl() : null,
                photo != null ? photo.googleMapsUri() : null
        );
    }

    private boolean matches(PlaceSummaryInfo summary, String normalizedKeyword) {
        return containsIgnoreCase(summary.name(), normalizedKeyword)
                || containsIgnoreCase(summary.address(), normalizedKeyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private String encodeCursor(VisitCursor cursor) {
        String value = String.join(
                CURSOR_DELIMITER,
                CURSOR_VERSION,
                cursor.purchaseDate().toString(),
                cursor.purchaseTime().toString(),
                cursor.consumptionId().toString()
        );
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private VisitCursor decodeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        if (cursor.length() > MAX_CURSOR_LENGTH) {
            throw createInvalidCursorException();
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(Pattern.quote(CURSOR_DELIMITER), -1);
            if (parts.length != CURSOR_PART_COUNT || !CURSOR_VERSION.equals(parts[0])) {
                throw createInvalidCursorException();
            }

            long consumptionId = Long.parseLong(parts[3]);
            if (consumptionId < 1) {
                throw createInvalidCursorException();
            }
            return new VisitCursor(
                    LocalDate.parse(parts[1]),
                    LocalTime.parse(parts[2]),
                    consumptionId
            );
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BusinessException(
                    ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_CURSOR,
                    exception
            );
        }
    }

    private BusinessException createInvalidCursorException() {
        return new BusinessException(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_CURSOR);
    }

    private record MatchedPlace(Consumption latestVisit, PlaceSummaryInfo summary) {
    }

    private record VisitCursor(
            LocalDate purchaseDate,
            LocalTime purchaseTime,
            Long consumptionId
    ) {

        private static VisitCursor from(Consumption consumption) {
            return new VisitCursor(
                    consumption.getPurchaseDate(),
                    consumption.getPurchaseTime(),
                    consumption.getId()
            );
        }
    }
}
