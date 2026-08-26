package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;
import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;
import kr.chapchap.place.application.port.GooglePlaceTextSearchPort;
import kr.chapchap.place.application.port.PlacePhotoPort;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GooglePlaceSearchServiceTest {

    private static final String PHOTO_NAME = "places/ChIJ123/photos/primary";
    private static final int THUMBNAIL_WIDTH = 400;

    @Mock
    private GooglePlaceTextSearchPort googlePlaceTextSearchPort;

    @Mock
    private PlacePhotoPort placePhotoPort;

    private GooglePlaceSearchService service;

    @BeforeEach
    void setUp() {
        service = new GooglePlaceSearchService(googlePlaceTextSearchPort, placePhotoPort);
    }

    @Test
    void 상호명과_주소로_Google_Place를_검색하고_기존_Photo_Media_호출로_썸네일을_조회한다() {
        // given
        GooglePlaceTextSearchInfo candidate = candidate(PHOTO_NAME);
        given(googlePlaceTextSearchPort.searchFirst(
                "투썸플레이스 신논현점 서울특별시 강남구 봉은사로 125"
        )).willReturn(Optional.of(candidate));
        given(placePhotoPort.resolvePhotoUri(PHOTO_NAME, THUMBNAIL_WIDTH))
                .willReturn(URI.create("https://lh3.googleusercontent.com/photo"));

        // when
        GooglePlaceSearchResultInfo result = service.search(
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125"
        ).orElseThrow();

        // then
        assertThat(result).isEqualTo(new GooglePlaceSearchResultInfo(
                "ChIJ123",
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                37.5065,
                127.0241,
                "https://lh3.googleusercontent.com/photo"
        ));
        then(placePhotoPort).should(never()).findPrimaryPhoto("ChIJ123");
    }

    @Test
    void 주소가_없으면_상호명만으로_장소를_검색한다() {
        // given
        given(googlePlaceTextSearchPort.searchFirst("투썸플레이스 신논현점"))
                .willReturn(Optional.of(candidate(null)));

        // when
        GooglePlaceSearchResultInfo result = service.search(
                " 투썸플레이스 신논현점 ",
                null
        ).orElseThrow();

        // then
        assertThat(result.googlePlaceId()).isEqualTo("ChIJ123");
        assertThat(result.thumbnailUrl()).isNull();
        then(placePhotoPort).shouldHaveNoInteractions();
    }

    @Test
    void 상호명이_없으면_장소를_검색하지_않는다() {
        // when
        Optional<GooglePlaceSearchResultInfo> result = service.search(
                " ",
                "서울특별시 강남구"
        );

        // then
        assertThat(result).isEmpty();
        then(googlePlaceTextSearchPort).shouldHaveNoInteractions();
        then(placePhotoPort).shouldHaveNoInteractions();
    }

    @Test
    void Text_Search_또는_Redis_연동에_실패하면_검색_결과를_반환하지_않는다() {
        // given
        given(googlePlaceTextSearchPort.searchFirst("투썸플레이스"))
                .willThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));

        // when
        Optional<GooglePlaceSearchResultInfo> result = service.search("투썸플레이스", null);

        // then
        assertThat(result).isEmpty();
        then(placePhotoPort).shouldHaveNoInteractions();
    }

    @Test
    void Text_Search_결과가_없으면_검색_결과를_반환하지_않는다() {
        // given
        given(googlePlaceTextSearchPort.searchFirst("없는 장소"))
                .willReturn(Optional.empty());

        // when
        Optional<GooglePlaceSearchResultInfo> result = service.search("없는 장소", null);

        // then
        assertThat(result).isEmpty();
        then(placePhotoPort).shouldHaveNoInteractions();
    }

    @Test
    void Photo_Media_조회에_실패해도_썸네일만_null인_검색_결과를_반환한다() {
        // given
        given(googlePlaceTextSearchPort.searchFirst("투썸플레이스"))
                .willReturn(Optional.of(candidate(PHOTO_NAME)));
        given(placePhotoPort.resolvePhotoUri(PHOTO_NAME, THUMBNAIL_WIDTH))
                .willThrow(new BusinessException(
                        PlaceErrorCode.PHOTO_REQUEST_LIMIT_EXCEEDED
                ));

        // when
        GooglePlaceSearchResultInfo result = service.search(
                "투썸플레이스",
                null
        ).orElseThrow();

        // then
        assertThat(result.googlePlaceId()).isEqualTo("ChIJ123");
        assertThat(result.thumbnailUrl()).isNull();
    }

    private GooglePlaceTextSearchInfo candidate(String photoName) {
        return new GooglePlaceTextSearchInfo(
                "ChIJ123",
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                37.5065,
                127.0241,
                photoName
        );
    }
}
