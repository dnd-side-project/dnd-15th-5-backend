package kr.chapchap.place;

import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.application.command.PlaceResolveCommand;
import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.application.port.AdministrativeDongLookupPort;
import kr.chapchap.place.application.service.PlaceResolveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PlaceResolvePersistenceIntegrationTest {

    private final PlaceResolveService placeResolveService;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AdministrativeDongLookupPort administrativeDongLookupPort;

    @Autowired
    PlaceResolvePersistenceIntegrationTest(
            PlaceResolveService placeResolveService,
            JdbcTemplate jdbcTemplate
    ) {
        this.placeResolveService = placeResolveService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM places");
    }

    @Test
    void 신규_장소는_SGIS_행정동과_프론트_좌표로_저장하고_다음_요청부터_재사용한다() {
        // given
        PlaceResolveCommand command = new PlaceResolveCommand(
                "ChIJ-place-persistence",
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                37.506481,
                127.024551
        );
        given(administrativeDongLookupPort.findByRoadAddress(command.roadAddress()))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return new AdministrativeDongInfo("11680650", "역삼1동");
                });

        // when
        Long createdId = placeResolveService.resolveOrCreate(command);
        Long reusedId = placeResolveService.resolveOrCreate(new PlaceResolveCommand(
                command.googlePlaceId(),
                "변경된 장소명",
                "서울특별시 강남구 다른로 1",
                37.0,
                126.0
        ));

        // then
        assertThat(reusedId).isEqualTo(createdId);
        Map<String, Object> place = jdbcTemplate.queryForMap(
                """
                        SELECT google_place_id,
                               name,
                               road_address,
                               administrative_dong_code,
                               administrative_dong_name,
                               ST_X(location::geometry) AS longitude,
                               ST_Y(location::geometry) AS latitude
                        FROM places
                        WHERE id = ?
                        """,
                createdId
        );
        assertThat(place.get("google_place_id")).isEqualTo(command.googlePlaceId());
        assertThat(place.get("name")).isEqualTo(command.name());
        assertThat(place.get("road_address")).isEqualTo(command.roadAddress());
        assertThat(place.get("administrative_dong_code")).isEqualTo("11680650");
        assertThat(place.get("administrative_dong_name")).isEqualTo("역삼1동");
        assertThat(((Number) place.get("longitude")).doubleValue()).isEqualTo(command.longitude());
        assertThat(((Number) place.get("latitude")).doubleValue()).isEqualTo(command.latitude());
        then(administrativeDongLookupPort).should().findByRoadAddress(command.roadAddress());
        then(administrativeDongLookupPort).shouldHaveNoMoreInteractions();
    }
}
