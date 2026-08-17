package kr.chapchap.consumption.infra.external;

import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.place.application.service.PlaceResolveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PlaceResolveAdapterTest {

    @Mock
    private PlaceResolveService placeResolveService;

    private PlaceResolveAdapter sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceResolveAdapter(placeResolveService);
    }

    @Test
    void 소비_장소_정보를_장소_모듈에_전달하고_장소_ID를_반환한다() {
        // given
        PlaceResolveCommand command = new PlaceResolveCommand(
                "ChIJxxxxxxxxxxxxxxxx",
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                37.506481,
                127.024551
        );
        given(placeResolveService.resolveOrCreate(
                org.mockito.ArgumentMatchers.any(kr.chapchap.place.application.command.PlaceResolveCommand.class)
        )).willReturn(15L);

        // when
        Long result = sut.resolve(command);

        // then
        assertThat(result).isEqualTo(15L);
        ArgumentCaptor<kr.chapchap.place.application.command.PlaceResolveCommand> commandCaptor =
                ArgumentCaptor.forClass(kr.chapchap.place.application.command.PlaceResolveCommand.class);
        then(placeResolveService).should().resolveOrCreate(commandCaptor.capture());
        kr.chapchap.place.application.command.PlaceResolveCommand placeCommand = commandCaptor.getValue();
        assertThat(placeCommand.googlePlaceId()).isEqualTo(command.googlePlaceId());
        assertThat(placeCommand.name()).isEqualTo(command.placeName());
        assertThat(placeCommand.roadAddress()).isEqualTo(command.roadAddress());
        assertThat(placeCommand.latitude()).isEqualTo(command.latitude());
        assertThat(placeCommand.longitude()).isEqualTo(command.longitude());
    }
}
