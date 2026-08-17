package kr.chapchap.place.application.service;

import kr.chapchap.place.application.command.PlaceResolveCommand;
import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.application.port.AdministrativeDongLookupPort;
import kr.chapchap.place.application.port.PlaceCreatePort;
import kr.chapchap.place.domain.entity.Place;
import kr.chapchap.place.domain.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceResolveServiceTest {

    private static final PlaceResolveCommand COMMAND = new PlaceResolveCommand(
            "google-place-id",
            "테스트 가게",
            "서울 강남구 테헤란로 1",
            37.5001,
            127.0365
    );

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private AdministrativeDongLookupPort administrativeDongLookupPort;

    @Mock
    private PlaceCreatePort placeCreatePort;

    private PlaceResolveService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceResolveService(
                placeRepository,
                administrativeDongLookupPort,
                placeCreatePort
        );
    }

    @Test
    void Google_Place_ID가_이미_있으면_SGIS를_호출하지_않고_기존_ID를_반환한다() {
        // given
        Place existingPlace = Place.create(
                COMMAND.googlePlaceId(),
                "기존 가게",
                "서울 강남구 기존로 1",
                "11680640",
                "역삼1동",
                37.49,
                127.03
        );
        ReflectionTestUtils.setField(existingPlace, "id", 101L);
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.of(existingPlace));

        // when
        Long result = sut.resolveOrCreate(COMMAND);

        // then
        assertThat(result).isEqualTo(101L);
        verifyNoInteractions(administrativeDongLookupPort, placeCreatePort);
    }

    @Test
    void 신규_장소면_도로명주소의_행정동과_프론트_위경도로_장소를_생성한다() {
        // given
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.empty());
        when(administrativeDongLookupPort.findByRoadAddress(COMMAND.roadAddress()))
                .thenReturn(new AdministrativeDongInfo("11680640", "역삼1동"));
        when(placeCreatePort.createOrGet(any(Place.class))).thenReturn(102L);

        // when
        Long result = sut.resolveOrCreate(COMMAND);

        // then
        assertThat(result).isEqualTo(102L);
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
        verify(placeCreatePort).createOrGet(captor.capture());
        Place createdPlace = captor.getValue();
        assertThat(createdPlace.getGooglePlaceId()).isEqualTo(COMMAND.googlePlaceId());
        assertThat(createdPlace.getName()).isEqualTo(COMMAND.name());
        assertThat(createdPlace.getRoadAddress()).isEqualTo(COMMAND.roadAddress());
        assertThat(createdPlace.getAdministrativeDongCode()).isEqualTo("11680640");
        assertThat(createdPlace.getAdministrativeDongName()).isEqualTo("역삼1동");
        assertThat(createdPlace.getLatitude()).isEqualTo(COMMAND.latitude());
        assertThat(createdPlace.getLongitude()).isEqualTo(COMMAND.longitude());
        assertThat(createdPlace.getLocation().getSRID()).isEqualTo(4326);
    }
}
