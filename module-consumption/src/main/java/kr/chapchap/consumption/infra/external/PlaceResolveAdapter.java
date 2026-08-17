package kr.chapchap.consumption.infra.external;

import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.port.PlaceResolvePort;
import kr.chapchap.place.application.service.PlaceResolveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PlaceResolveAdapter implements PlaceResolvePort {

    private final PlaceResolveService placeResolveService;

    @Override
    public Long resolve(PlaceResolveCommand command) {
        kr.chapchap.place.application.command.PlaceResolveCommand placeCommand =
                new kr.chapchap.place.application.command.PlaceResolveCommand(
                        command.googlePlaceId(),
                        command.placeName(),
                        command.roadAddress(),
                        command.latitude(),
                        command.longitude()
                );
        return placeResolveService.resolveOrCreate(placeCommand);
    }
}
