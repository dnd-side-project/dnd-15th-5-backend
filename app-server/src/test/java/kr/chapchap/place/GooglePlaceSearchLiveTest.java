package kr.chapchap.place;

import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;
import kr.chapchap.place.application.service.GooglePlaceSearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_GOOGLE_PLACE_SEARCH_LIVE_TEST", matches = "true")
class GooglePlaceSearchLiveTest {

    private static final long REQUEST_INTERVAL_MS = 2_100;

    private final GooglePlaceSearchService googlePlaceSearchService;

    @Autowired
    GooglePlaceSearchLiveTest(GooglePlaceSearchService googlePlaceSearchService) {
        this.googlePlaceSearchService = googlePlaceSearchService;
    }

    @Test
    void 실제_Google_API로_정상_및_OCR_오인식_검색어_20건을_확인한다() throws InterruptedException {
        // given
        List<SearchCase> cases = List.of(
                new SearchCase("투썸플레이스 신논현점", "서울특별시 강남구 봉은사로 125 1층", "정상_상호명_주소"),
                new SearchCase("투썸플레이스 신논현점", null, "정상_상호명"),
                new SearchCase("투썸플레O스 신논현점", "서울특별시 강남구 봉은사로 125 1층", "오인식_상호명_주소"),
                new SearchCase("투썸플레O스 신논현점", null, "오인식_상호명"),
                new SearchCase("교보문고 광화문점", "서울특별시 종로구 종로 1", "정상_상호명_주소"),
                new SearchCase("교보문고 광화문점", null, "정상_상호명"),
                new SearchCase("교보문고 광화문접", "서울특별시 종로구 종로 1", "오인식_상호명_주소"),
                new SearchCase("교보문고 광화문접", null, "오인식_상호명"),
                new SearchCase("스타벅스 더종로R점", "서울특별시 종로구 종로 51", "정상_상호명_주소"),
                new SearchCase("스타벅스 더종로R점", null, "정상_상호명"),
                new SearchCase("스타벅스 더종로R전", "서울특별시 종로구 종로 51", "오인식_상호명_주소"),
                new SearchCase("스타벅스 더종로R전", null, "오인식_상호명"),
                new SearchCase("이마트 용산점", "서울특별시 용산구 한강대로23길 55", "정상_상호명_주소"),
                new SearchCase("이마트 용산점", null, "정상_상호명"),
                new SearchCase("이마트 용산전", "서울특별시 용산구 한강대로23길 55", "오인식_상호명_주소"),
                new SearchCase("이마트 용산전", null, "오인식_상호명"),
                new SearchCase("성심당 본점", "대전광역시 중구 대종로480번길 15", "정상_상호명_주소"),
                new SearchCase("성심당 본점", null, "정상_상호명"),
                new SearchCase("성심당 본접", "대전광역시 중구 대종로480번길 15", "오인식_상호명_주소"),
                new SearchCase("성심당 본접", null, "오인식_상호명")
        );
        int resultCount = 0;
        int thumbnailCount = 0;

        // when
        for (int index = 0; index < cases.size(); index++) {
            SearchCase searchCase = cases.get(index);
            Optional<GooglePlaceSearchResultInfo> result = googlePlaceSearchService.search(
                    searchCase.storeName(),
                    searchCase.address()
            );

            if (result.isPresent()) {
                GooglePlaceSearchResultInfo place = result.get();
                resultCount++;
                if (place.thumbnailUrl() != null) {
                    thumbnailCount++;
                }
                log.info(
                        "LIVE_PLACE_RESULT index={}, type={}, inputStoreName={}, inputAddress={}, "
                                + "googlePlaceId={}, placeName={}, roadAddress={}, latitude={}, longitude={}, thumbnail={}",
                        index + 1,
                        searchCase.type(),
                        searchCase.storeName(),
                        searchCase.address(),
                        place.googlePlaceId(),
                        place.placeName(),
                        place.roadAddress(),
                        place.latitude(),
                        place.longitude(),
                        place.thumbnailUrl() != null
                );
            } else {
                log.info(
                        "LIVE_PLACE_RESULT index={}, type={}, inputStoreName={}, inputAddress={}, result=EMPTY",
                        index + 1,
                        searchCase.type(),
                        searchCase.storeName(),
                        searchCase.address()
                );
            }

            if (index < cases.size() - 1) {
                Thread.sleep(REQUEST_INTERVAL_MS);
            }
        }

        // then
        log.info(
                "LIVE_PLACE_SUMMARY attempts={}, results={}, thumbnails={}",
                cases.size(),
                resultCount,
                thumbnailCount
        );
        assertThat(resultCount).isEqualTo(cases.size());
        assertThat(thumbnailCount).isEqualTo(cases.size());
    }

    private record SearchCase(String storeName, String address, String type) {
    }
}
