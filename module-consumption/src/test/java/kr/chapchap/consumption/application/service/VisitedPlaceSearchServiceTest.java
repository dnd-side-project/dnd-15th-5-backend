package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.VisitedPlaceSearchCommand;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.service.PlacePhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class VisitedPlaceSearchServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 8, 21);
    private static final LocalTime VISIT_TIME = LocalTime.of(12, 30);

    @Mock
    private ConsumptionQueryRepository consumptionQueryRepository;

    @Mock
    private PlaceSummaryLookupPort placeSummaryLookupPort;

    @Mock
    private PlacePhotoService placePhotoService;

    private VisitedPlaceSearchService service;

    @BeforeEach
    void setUp() {
        service = new VisitedPlaceSearchService(
                consumptionQueryRepository,
                placeSummaryLookupPort,
                placePhotoService
        );
    }

    @Test
    void 방문_장소를_검색할_때_결과가_size를_초과하면_5개_장소만_사진을_조회하고_nextCursor를_반환한다() {
        // given
        List<Consumption> latestVisits = List.of(
                consumption(106L, 206L),
                consumption(105L, 205L),
                consumption(104L, 204L),
                consumption(103L, 203L),
                consumption(102L, 202L),
                consumption(101L, 201L)
        );
        given(consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                USER_ID, null, null, null, 500
        )).willReturn(latestVisits);
        Map<Long, PlaceSummaryInfo> summaries = new LinkedHashMap<>();
        for (Consumption visit : latestVisits) {
            summaries.put(
                    visit.getPlaceId(),
                    summary("카페 " + visit.getPlaceId(), "서울", "google-" + visit.getPlaceId())
            );
        }
        given(placeSummaryLookupPort.findSummaries(any())).willReturn(summaries);
        given(placePhotoService.findThumbnails(any())).willAnswer(invocation -> {
            Map<Long, String> requested = invocation.getArgument(0);
            Map<Long, PlacePhotoInfo> photos = new LinkedHashMap<>();
            requested.forEach((placeId, googlePlaceId) -> photos.put(
                    placeId,
                    new PlacePhotoInfo(
                            "https://lh3.googleusercontent.com/photo/" + placeId,
                            "https://maps.google.com/photo/" + placeId
                    )
            ));
            return photos;
        });

        // when
        VisitedPlaceSearchInfo result = service.search(
                new VisitedPlaceSearchCommand(USER_ID, "카페", null, 5)
        );

        // then
        assertThat(result.places()).extracting(VisitedPlaceSearchInfo.VisitedPlaceInfo::placeId)
                .containsExactly(206L, 205L, 204L, 203L, 202L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotBlank();
        assertThat(result.places().get(0)).satisfies(place -> {
            assertThat(place.thumbnailUrl()).isEqualTo(
                    "https://lh3.googleusercontent.com/photo/206"
            );
            assertThat(place.googleMapsUri()).isEqualTo("https://maps.google.com/photo/206");
        });
        ArgumentCaptor<Map<Long, String>> photoRequestCaptor = ArgumentCaptor.forClass(Map.class);
        then(placePhotoService).should().findThumbnails(photoRequestCaptor.capture());
        assertThat(photoRequestCaptor.getValue().keySet())
                .containsExactly(206L, 205L, 204L, 203L, 202L);
    }

    @Test
    void 방문_장소를_검색할_때_첫_500개에_keyword와_일치하는_결과가_없으면_다음_배치에서_address가_부분_일치하는_장소를_반환한다() {
        // given
        List<Consumption> firstBatch = java.util.stream.LongStream.rangeClosed(1, 500)
                .mapToObj(sequence -> consumption(1001L - sequence, 2000L - sequence))
                .toList();
        Consumption addressMatch = consumption(500L, 1499L);
        given(consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                USER_ID, null, null, null, 500
        )).willReturn(firstBatch);
        Consumption lastScanned = firstBatch.get(firstBatch.size() - 1);
        given(consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                USER_ID,
                lastScanned.getPurchaseDate(),
                lastScanned.getPurchaseTime(),
                lastScanned.getId(),
                500
        )).willReturn(List.of(addressMatch));
        Map<Long, PlaceSummaryInfo> firstSummaries = new LinkedHashMap<>();
        firstBatch.forEach(visit -> firstSummaries.put(
                visit.getPlaceId(),
                summary("일반 가게", "부산 해운대구", null)
        ));
        given(placeSummaryLookupPort.findSummaries(any()))
                .willReturn(
                        firstSummaries,
                        Map.of(addressMatch.getPlaceId(), summary(
                                "파스타집", "서울 성동구 성수이로", null
                        ))
                );

        // when
        VisitedPlaceSearchInfo result = service.search(
                new VisitedPlaceSearchCommand(USER_ID, "  성수  ", null, 5)
        );

        // then
        assertThat(result.places()).singleElement()
                .satisfies(place -> {
                    assertThat(place.placeName()).isEqualTo("파스타집");
                    assertThat(place.roadAddress()).contains("성수");
                    assertThat(place.thumbnailUrl()).isNull();
                    assertThat(place.googleMapsUri()).isNull();
                });
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        then(placePhotoService).should(never()).findThumbnails(any());
    }

    @Test
    void nextCursor로_방문_장소를_검색할_때_이전_페이지의_마지막_장소_다음부터_조회한다() {
        // given
        List<Consumption> firstPage = List.of(
                consumption(106L, 206L),
                consumption(105L, 205L),
                consumption(104L, 204L),
                consumption(103L, 203L),
                consumption(102L, 202L),
                consumption(101L, 201L)
        );
        given(consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                USER_ID, null, null, null, 500
        )).willReturn(firstPage);
        Map<Long, PlaceSummaryInfo> summaries = new LinkedHashMap<>();
        firstPage.forEach(visit -> summaries.put(
                visit.getPlaceId(),
                summary("카페", "서울", null)
        ));
        given(placeSummaryLookupPort.findSummaries(any())).willReturn(summaries);

        VisitedPlaceSearchInfo firstResult = service.search(
                new VisitedPlaceSearchCommand(USER_ID, "카페", null, 5)
        );
        given(consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                USER_ID, VISIT_DATE, VISIT_TIME, 102L, 500
        )).willReturn(List.of());

        // when
        service.search(new VisitedPlaceSearchCommand(USER_ID, "카페", firstResult.nextCursor(), 5));

        // then
        then(consumptionQueryRepository).should().searchLatestVisitedPlacesByCursor(
                USER_ID, VISIT_DATE, VISIT_TIME, 102L, 500
        );
    }

    @Test
    void 손상된_cursor로_방문_장소를_검색할_때_예외를_던지고_저장소를_조회하지_않는다() {
        // when & then
        assertThatThrownBy(() -> service.search(
                new VisitedPlaceSearchCommand(USER_ID, "카페", "invalid-cursor", 5)
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_CURSOR);
        then(consumptionQueryRepository).shouldHaveNoInteractions();
        then(placeSummaryLookupPort).shouldHaveNoInteractions();
        then(placePhotoService).shouldHaveNoInteractions();
    }

    private Consumption consumption(Long consumptionId, Long placeId) {
        Consumption consumption = Consumption.builder()
                .userId(USER_ID)
                .placeId(placeId)
                .purchaseDate(VISIT_DATE)
                .purchaseTime(VISIT_TIME)
                .amount(10_000L)
                .category("카페")
                .stickerItemId(1L)
                .build();
        ReflectionTestUtils.setField(consumption, "id", consumptionId);
        return consumption;
    }

    private PlaceSummaryInfo summary(
            String name,
            String address,
            String googlePlaceId
    ) {
        return new PlaceSummaryInfo(
                name,
                "성수동",
                address,
                37.5,
                127.0,
                googlePlaceId
        );
    }
}
