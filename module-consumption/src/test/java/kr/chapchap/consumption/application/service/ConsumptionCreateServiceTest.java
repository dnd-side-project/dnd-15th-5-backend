package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;
import kr.chapchap.consumption.application.port.PlaceResolvePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ConsumptionCreateServiceTest {

    @Mock
    private PlaceResolvePort placeResolvePort;

    @Mock
    private ConsumptionCommandService consumptionCommandService;

    @Test
    void 장소를_먼저_해결한_뒤_소비_저장_트랜잭션을_호출한다() {
        // given
        PlaceResolveCommand place = new PlaceResolveCommand(
                "google-place-id",
                "찹찹카페",
                "서울특별시 강남구 테헤란로 123",
                37.501,
                127.039
        );
        ConsumptionCreateCommand command = new ConsumptionCreateCommand(
                1L,
                null,
                place,
                LocalDate.of(2026, 8, 16),
                LocalTime.of(11, 30),
                12_000L,
                "카페"
        );
        ConsumptionCreateInfo expected = new ConsumptionCreateInfo(
                10L,
                "공통",
                "눈"
        );
        given(placeResolvePort.resolve(place)).willReturn(101L);
        given(consumptionCommandService.create(command, 101L)).willReturn(expected);
        ConsumptionCreateService service = new ConsumptionCreateService(
                placeResolvePort,
                consumptionCommandService
        );

        // when
        ConsumptionCreateInfo result = service.create(command);

        // then
        assertThat(result).isEqualTo(expected);
        InOrder inOrder = inOrder(placeResolvePort, consumptionCommandService);
        inOrder.verify(placeResolvePort).resolve(place);
        inOrder.verify(consumptionCommandService).create(command, 101L);
    }
}
