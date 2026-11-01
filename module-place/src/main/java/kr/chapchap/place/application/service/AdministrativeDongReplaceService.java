package kr.chapchap.place.application.service;

import kr.chapchap.place.domain.entity.AdministrativeDong;
import kr.chapchap.place.domain.repository.AdministrativeDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AdministrativeDongReplaceService {

    private static final int SAVE_CHUNK_SIZE = 500;

    private final AdministrativeDongRepository administrativeDongRepository;


    @Transactional
    public void replaceAll(List<AdministrativeDong> administrativeDongs) {
        administrativeDongRepository.deleteAllInBatch();
        for (int i = 0; i < administrativeDongs.size(); i += SAVE_CHUNK_SIZE) {
            List<AdministrativeDong> chunk =
                    administrativeDongs.subList(i, Math.min(i + SAVE_CHUNK_SIZE, administrativeDongs.size()));
            administrativeDongRepository.saveAll(chunk);
        }
    }
}
