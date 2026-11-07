package kr.chapchap.place.application.service;

import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.application.port.AdministrativeDongLookupPort;
import kr.chapchap.place.domain.entity.AdministrativeDong;
import kr.chapchap.place.domain.repository.AdministrativeDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdministrativeDongQueryService {

    private final AdministrativeDongLookupPort administrativeDongLookupPort;
    private final AdministrativeDongRepository administrativeDongRepository;

    public String findFullNameByCoordinates(double latitude, double longitude) {
        AdministrativeDongInfo dong = administrativeDongLookupPort.findByCoordinates(latitude, longitude);
        return administrativeDongRepository.findById(dong.code())
                .map(AdministrativeDong::getFullName)
                .orElse(dong.name());
    }
}
