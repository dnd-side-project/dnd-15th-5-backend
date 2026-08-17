package kr.chapchap.consumption.application.port;

import kr.chapchap.consumption.application.command.PlaceResolveCommand;

public interface PlaceResolvePort {

    Long resolve(PlaceResolveCommand command);
}
