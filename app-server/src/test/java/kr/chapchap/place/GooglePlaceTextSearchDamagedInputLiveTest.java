package kr.chapchap.place;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;
import kr.chapchap.place.application.port.GooglePlaceTextSearchPort;
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
@EnabledIfEnvironmentVariable(
        named = "RUN_GOOGLE_PLACES_DAMAGED_INPUT_LIVE_TEST",
        matches = "true"
)
class GooglePlaceTextSearchDamagedInputLiveTest {

    private static final long REQUEST_INTERVAL_MS = 2_100;

    private final GooglePlaceTextSearchPort googlePlaceTextSearchPort;

    @Autowired
    GooglePlaceTextSearchDamagedInputLiveTest(
            GooglePlaceTextSearchPort googlePlaceTextSearchPort
    ) {
        this.googlePlaceTextSearchPort = googlePlaceTextSearchPort;
    }

    /*
     * 2026-08-26 languageCode=ko 실제 호출 결과
     *
     * - 의도한 장소 복구 16건: 2~10, 12~14, 16~17, 19~20
     * - 애매한 결과 1건: 1번은 같은 체인이지만 시청역2호선점 대신 서소문점 반환
     * - 잘못된 장소 3건
     *   - 11번 광천식당 -> 베니키아 호텔 대림
     *   - 15번 투썸플레이스 신논현점 -> 성심당 본점
     *   - 18번 하동관 명동본점 -> 하수오곰탕 명동점
     * - 결과 없음 0건, Google 및 Redis 오류 0건
     * - Text Search 20회, Photo Media 및 Place Details 호출 없음
     *
     * 상호와 주소가 많이 손상돼도 결과가 null이 되진 않았고,
     * 드물게 주소나 업종이 비슷한 다른 장소가 반환되기도 함
     */
    @Test
    void 실제_Google_API로_심하게_손상된_OCR_검색어_20건을_확인한다() throws InterruptedException {
        // given
        List<SearchCase> cases = List.of(
                new SearchCase(
                        "세븐일레븐 시청역2호선점",
                        "세븐일레븐",
                        "서울 중구 서소문",
                        "체인 지점명과 주소 뒷부분 유실"
                ),
                new SearchCase(
                        "교보문고 광화문점",
                        "교보문",
                        "교보문고 광화",
                        "상호 절반 유실과 주소 필드의 상호 혼입"
                ),
                new SearchCase(
                        "스타벅스 더종로R점",
                        "스 타 벅 ㅅ 더 종 로",
                        "서울 종로구 종로 5l",
                        "자모 분리와 과도한 공백 및 숫자 영문 혼동"
                ),
                new SearchCase(
                        "이마트 용산점",
                        "서울 용산구 한강대",
                        "이마트 용산",
                        "상호와 주소 필드 뒤바뀜"
                ),
                new SearchCase(
                        "롯데리아 서울역사점",
                        "LOTTERlA 서을역ㅅ",
                        "용산 한강대로405 2ㅊ",
                        "영문 로고 혼합과 영문 및 한글 오인식"
                ),
                new SearchCase(
                        "올리브영 명동타운",
                        "(주)씨제이올리브 명동",
                        "중구 명동길 53 영수증",
                        "매장명 대신 법인명과 문서 문구 인식"
                ),
                new SearchCase(
                        "다이소 명동역점",
                        "다이소 명동역점 다이소 명",
                        "서울중구퇴계로134-1TEL02",
                        "상호 중복과 주소 공백 소실 및 전화번호 결합"
                ),
                new SearchCase(
                        "우래옥 본점",
                        "우래",
                        "서울 중구 창경궁로 62",
                        "독립 상호 절반과 건물번호 뒷부분 유실"
                ),
                new SearchCase(
                        "진주회관",
                        "서울 중구 세종대로11",
                        "진주회",
                        "필드 뒤바뀜과 양쪽 값 절단"
                ),
                new SearchCase(
                        "자하손만두",
                        "자하손",
                        "서울 종로구 백석동",
                        "상호 절단과 도로명의 행정동 오인식"
                ),
                new SearchCase(
                        "광천식당",
                        "광정",
                        "대전 중구 대종로 505",
                        "상호 글자 치환과 도로명 및 번지 구조 훼손"
                ),
                new SearchCase(
                        "희락반점",
                        "희락반",
                        "대전 중구 중앙로129번",
                        "독립 상호 마지막 글자와 도로명 뒷부분 유실"
                ),
                new SearchCase(
                        "대성집",
                        "대정침",
                        "서울 종로구 사직로 S",
                        "여러 글자 자형 오인식과 숫자 영문 혼동"
                ),
                new SearchCase(
                        "신발원",
                        "고기만두 신발",
                        "부산 동구 대영로243",
                        "품목명 혼입과 상호 및 주소 뒷부분 유실"
                ),
                new SearchCase(
                        "투썸플레이스 신논현점",
                        "플레이스 신논",
                        "대전 중구 대종로480",
                        "브랜드 앞부분 유실과 무관한 주소 결합"
                ),
                new SearchCase(
                        "성심당 본점",
                        "튀김소보로 3개 10000원 성심",
                        "대전 중구 대종로480",
                        "메뉴 및 금액 혼입과 상호 및 주소 절단"
                ),
                new SearchCase(
                        "런던베이글뮤지엄 안국점",
                        "LONDON BAG",
                        "종로 북촌",
                        "영문 상호 절반과 주소 대부분 유실"
                ),
                new SearchCase(
                        "하동관 명동본점",
                        "곰탕 특 15000",
                        "서울 중구 명동9",
                        "상호 전체 유실과 메뉴 및 금액 대체"
                ),
                new SearchCase(
                        "정돈 대학로본점",
                        "정돈 대",
                        "03079",
                        "상호 및 지점명 절단과 우편번호만 인식"
                ),
                new SearchCase(
                        "소문난성수감자탕",
                        "소문난성수감자 합계 42,000",
                        "서울 성동구 연무",
                        "상호 및 도로명 절단과 합계 금액 혼입"
                )
        );
        int resultCount = 0;
        int emptyCount = 0;
        int errorCount = 0;

        // when
        for (int index = 0; index < cases.size(); index++) {
            SearchCase searchCase = cases.get(index);
            String textQuery = searchCase.storeName() + " " + searchCase.address();

            try {
                Optional<GooglePlaceTextSearchInfo> result =
                        googlePlaceTextSearchPort.searchFirst(textQuery);
                if (result.isPresent()) {
                    GooglePlaceTextSearchInfo place = result.get();
                    resultCount++;
                    log.info(
                            "LIVE_DAMAGED_PLACE_RESULT index={}, expectedPlace={}, damageType={}, "
                                    + "inputStoreName={}, inputAddress={}, googlePlaceId={}, "
                                    + "placeName={}, roadAddress={}, latitude={}, longitude={}",
                            index + 1,
                            searchCase.expectedPlace(),
                            searchCase.damageType(),
                            searchCase.storeName(),
                            searchCase.address(),
                            place.googlePlaceId(),
                            place.placeName(),
                            place.roadAddress(),
                            place.latitude(),
                            place.longitude()
                    );
                } else {
                    emptyCount++;
                    log.info(
                            "LIVE_DAMAGED_PLACE_RESULT index={}, expectedPlace={}, damageType={}, "
                                    + "inputStoreName={}, inputAddress={}, result=EMPTY",
                            index + 1,
                            searchCase.expectedPlace(),
                            searchCase.damageType(),
                            searchCase.storeName(),
                            searchCase.address()
                    );
                }
            } catch (BusinessException exception) {
                errorCount++;
                log.warn(
                        "LIVE_DAMAGED_PLACE_RESULT index={}, expectedPlace={}, damageType={}, "
                                + "inputStoreName={}, inputAddress={}, errorCode={}",
                        index + 1,
                        searchCase.expectedPlace(),
                        searchCase.damageType(),
                        searchCase.storeName(),
                        searchCase.address(),
                        exception.getErrorCode().getCode()
                );
            }

            if (index < cases.size() - 1) {
                Thread.sleep(REQUEST_INTERVAL_MS);
            }
        }

        // then
        log.info(
                "LIVE_DAMAGED_PLACE_SUMMARY attempts={}, results={}, empty={}, errors={}",
                cases.size(),
                resultCount,
                emptyCount,
                errorCount
        );
        assertThat(errorCount).isZero();
        assertThat(resultCount + emptyCount).isEqualTo(cases.size());
    }

    private record SearchCase(
            String expectedPlace,
            String storeName,
            String address,
            String damageType
    ) {
    }
}
