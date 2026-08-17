package kr.chapchap.place.application.service;

import kr.chapchap.place.application.command.PlaceResolveCommand;
import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.application.port.AdministrativeDongLookupPort;
import kr.chapchap.place.application.port.PlaceCreatePort;
import kr.chapchap.place.domain.entity.Place;
import kr.chapchap.place.domain.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class PlaceResolveService {

    private final PlaceRepository placeRepository;
    private final AdministrativeDongLookupPort administrativeDongLookupPort;
    private final PlaceCreatePort placeCreatePort;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long resolveOrCreate(PlaceResolveCommand command) {
        Objects.requireNonNull(command, "장소 확인 요청은 필수입니다.");

        return placeRepository.findByGooglePlaceId(command.googlePlaceId())
                .map(Place::getId)
                .orElseGet(() -> createPlace(command));
    }

    private Long createPlace(PlaceResolveCommand command) {
        AdministrativeDongInfo administrativeDong =
                administrativeDongLookupPort.findByRoadAddress(command.roadAddress());
        Place place = Place.create(
                command.googlePlaceId(),
                command.name(),
                command.roadAddress(),
                administrativeDong.code(),
                administrativeDong.name(),
                command.latitude(),
                command.longitude()
        );
        return placeCreatePort.createOrGet(place);
    }
}
