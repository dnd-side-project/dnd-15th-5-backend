package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;
import kr.chapchap.consumption.application.port.PlaceResolvePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConsumptionCreateService {

    private final PlaceResolvePort placeResolvePort;
    private final ConsumptionCommandService consumptionCommandService;

    public ConsumptionCreateInfo create(ConsumptionCreateCommand command) {
        Long placeId = placeResolvePort.resolve(command.place());
        return consumptionCommandService.create(command, placeId);
    }
}
