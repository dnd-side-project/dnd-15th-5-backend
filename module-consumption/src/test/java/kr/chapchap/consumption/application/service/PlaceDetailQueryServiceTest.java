package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.PlaceVisitSearchCommand;
import kr.chapchap.consumption.application.info.PlaceDetailInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StickerCountInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StickerInfo;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo;
import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo.VisitInfo;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.PlaceImageRecordRow;
import kr.chapchap.consumption.domain.entity.PlaceRecentRecordRow;
import kr.chapchap.consumption.domain.entity.PlaceVisitStatsRow;
import kr.chapchap.consumption.domain.entity.StickerCountRow;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.domain.service.PlaceVisitCommentGenerator;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceDetailQueryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-08-13T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private ConsumptionQueryRepository consumptionQueryRepository;

    @Mock
    private StickerQueryService stickerQueryService;

    @Mock
    private PlaceSummaryLookupPort placeSummaryLookupPort;

    @Mock
    private PlaceVisitCommentGenerator placeVisitCommentGenerator;

    @Mock
    private ConsumptionImageRepository consumptionImageRepository;

    @Mock
    private ConsumptionImageStorage consumptionImageStorage;

    @Mock
    private PlaceThumbnailBatchLookup placeThumbnailBatchLookup;

    @Mock
    private StickerItem stickerItem;

    @Mock
    private ConsumptionImage consumptionImage;

    @Mock
    private ConsumptionImage consumptionImage2;

    private PlaceDetailQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceDetailQueryService(
                consumptionQueryRepository, stickerQueryService, placeSummaryLookupPort,
                placeVisitCommentGenerator, consumptionImageRepository, consumptionImageStorage,
                placeThumbnailBatchLookup, fixedClock);
    }

    @Test
    void 최근_기록_썸네일_줄은_스티커와_사진_기록을_순서대로_섞어서_반환한다() {
        // given
        when(consumptionQueryRepository.findVisitStats(1L, 101L))
                .thenReturn(java.util.Optional.of(new PlaceVisitStatsRow("카페", 2L, LocalDate.of(2026, 1, 1))));
        when(consumptionQueryRepository.countVisits(eq(1L), eq(101L), any(), any())).thenReturn(0L);
        when(consumptionQueryRepository.aggregatePlaceRankingByCursor(eq(1L), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(placeSummaryLookupPort.findSummaries(List.of(101L))).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557)
        ));
        when(placeVisitCommentGenerator.generate(2)).thenReturn("댓글");
        when(consumptionQueryRepository.aggregateStickerCountsByPlace(1L, 101L)).thenReturn(List.of());

        // 최신순으로 사진 기록(imageId=7) 다음 스티커 기록(stickerItemId=3)
        when(consumptionQueryRepository.findRecentRecordsByPlace(eq(1L), eq(101L), anyInt())).thenReturn(List.of(
                new PlaceRecentRecordRow(null, 7L, "편의점/마트", LocalDate.of(2026, 8, 10)),
                new PlaceRecentRecordRow(3L, null, "카페", LocalDate.of(2026, 8, 1))
        ));
        when(consumptionImageRepository.findAllByIdInAndUserId(List.of(7L), 1L)).thenReturn(List.of(consumptionImage));
        when(consumptionImage.getId()).thenReturn(7L);
        when(consumptionImage.getObjectKey()).thenReturn("consumption-images/7");
        when(consumptionImageStorage.createReadUrl("consumption-images/7")).thenReturn("https://cdn.example.com/7");
        when(stickerQueryService.findItems(List.of(3L))).thenReturn(Map.of(3L, stickerItem));
        when(stickerItem.getName()).thenReturn("도넛");

        // when
        PlaceDetailInfo result = sut.getDetail(1L, 101L);

        // then
        assertThat(result.recentStickers()).hasSize(2);
        StickerInfo photoItem = result.recentStickers().get(0);
        assertThat(photoItem.imageUrl()).isEqualTo("https://cdn.example.com/7");
        assertThat(photoItem.category()).isEqualTo("편의점/마트");
        assertThat(photoItem.itemName()).isNull();

        StickerInfo stickerInfoItem = result.recentStickers().get(1);
        assertThat(stickerInfoItem.category()).isEqualTo("카페");
        assertThat(stickerInfoItem.itemName()).isEqualTo("도넛");
        assertThat(stickerInfoItem.imageUrl()).isNull();
    }

    @Test
    void 모아보기는_스티커는_타입별_집계_사진은_장당_1개씩_나열한다() {
        // given
        when(consumptionQueryRepository.findVisitStats(1L, 101L))
                .thenReturn(java.util.Optional.of(new PlaceVisitStatsRow("카페", 4L, LocalDate.of(2026, 1, 1))));
        when(consumptionQueryRepository.countVisits(eq(1L), eq(101L), any(), any())).thenReturn(0L);
        when(consumptionQueryRepository.aggregatePlaceRankingByCursor(eq(1L), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(placeSummaryLookupPort.findSummaries(List.of(101L))).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557)
        ));
        when(placeVisitCommentGenerator.generate(4)).thenReturn("댓글");
        when(consumptionQueryRepository.findRecentRecordsByPlace(eq(1L), eq(101L), anyInt())).thenReturn(List.of());
        when(stickerQueryService.findItems(List.of())).thenReturn(Map.of());

        // 스티커(도넛) 2개 집계
        when(consumptionQueryRepository.aggregateStickerCountsByPlace(1L, 101L))
                .thenReturn(List.of(new StickerCountRow(3L, 2L)));
        when(stickerQueryService.findItems(List.of(3L))).thenReturn(Map.of(3L, stickerItem));
        when(stickerItem.getCategory()).thenReturn("카페");
        when(stickerItem.getName()).thenReturn("도넛");

        // 사진 2장 (각각 고유, count=1씩)
        when(consumptionQueryRepository.findAllImageRecordsByPlace(1L, 101L)).thenReturn(List.of(
                new PlaceImageRecordRow(7L, "편의점/마트"),
                new PlaceImageRecordRow(8L, "음식점")
        ));
        when(consumptionImageRepository.findAllByIdInAndUserId(List.of(7L, 8L), 1L))
                .thenReturn(List.of(consumptionImage, consumptionImage2));
        when(consumptionImage.getId()).thenReturn(7L);
        when(consumptionImage.getObjectKey()).thenReturn("consumption-images/7");
        when(consumptionImageStorage.createReadUrl("consumption-images/7")).thenReturn("https://cdn.example.com/7");
        when(consumptionImage2.getId()).thenReturn(8L);
        when(consumptionImage2.getObjectKey()).thenReturn("consumption-images/8");
        when(consumptionImageStorage.createReadUrl("consumption-images/8")).thenReturn("https://cdn.example.com/8");

        // when
        PlaceDetailInfo result = sut.getDetail(1L, 101L);

        // then
        assertThat(result.stickerSummary()).hasSize(3);
        StickerCountInfo stickerType = result.stickerSummary().get(0);
        assertThat(stickerType.category()).isEqualTo("카페");
        assertThat(stickerType.itemName()).isEqualTo("도넛");
        assertThat(stickerType.count()).isEqualTo(2L);
        assertThat(stickerType.imageUrl()).isNull();

        assertThat(result.stickerSummary().subList(1, 3))
                .extracting(StickerCountInfo::category, StickerCountInfo::imageUrl, StickerCountInfo::count)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("편의점/마트", "https://cdn.example.com/7", 1L),
                        org.assertj.core.groups.Tuple.tuple("음식점", "https://cdn.example.com/8", 1L)
                );
    }

    @Test
    void 방문_기록_리스트는_같은_가게의_구글_썸네일_1장을_공통으로_반환한다() {
        // given
        Consumption firstVisit = Consumption.create(
                1L, 101L, LocalDate.of(2026, 8, 10), LocalTime.of(20, 0),
                5500L, "카페", 3L, null, null);
        Consumption secondVisit = Consumption.create(
                1L, 101L, LocalDate.of(2026, 8, 1), LocalTime.of(19, 0),
                4800L, "카페", 3L, null, null);
        when(consumptionQueryRepository.searchPlaceVisitsByCursor(
                eq(1L), eq(101L), any(), any(), any(), anyInt()))
                .thenReturn(List.of(firstVisit, secondVisit));
        when(placeSummaryLookupPort.findSummaries(List.of(101L))).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557, "ChIJtest101")
        ));
        when(placeThumbnailBatchLookup.findThumbnails(Map.of(101L, "ChIJtest101"))).thenReturn(Map.of(
                101L, new PlacePhotoInfo("https://cdn.example.com/place-101.jpg", "https://maps.google.com/place-101")
        ));

        // when
        PlaceVisitScrollInfo result = sut.getVisits(new PlaceVisitSearchCommand(1L, 101L, null, null, null, 5));

        // then
        assertThat(result.visits()).hasSize(2);
        assertThat(result.thumbnailUrl()).isEqualTo("https://cdn.example.com/place-101.jpg");
        // 구글 썸네일 조회는 방문 기록 수와 무관하게 가게당 1번만 호출된다
        org.mockito.Mockito.verify(placeThumbnailBatchLookup, org.mockito.Mockito.times(1))
                .findThumbnails(Map.of(101L, "ChIJtest101"));
        org.mockito.Mockito.verifyNoInteractions(consumptionImageRepository, consumptionImageStorage, stickerQueryService);
    }

    @Test
    void 구글_place_id가_없는_가게는_방문_기록_썸네일이_null이다() {
        // given
        when(consumptionQueryRepository.searchPlaceVisitsByCursor(
                eq(1L), eq(101L), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(placeSummaryLookupPort.findSummaries(List.of(101L))).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557)
        ));

        // when
        PlaceVisitScrollInfo result = sut.getVisits(new PlaceVisitSearchCommand(1L, 101L, null, null, null, 5));

        // then
        assertThat(result.thumbnailUrl()).isNull();
        org.mockito.Mockito.verifyNoInteractions(placeThumbnailBatchLookup);
    }
}
