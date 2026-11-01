package kr.chapchap.place.application.service;

import kr.chapchap.place.application.info.UserTownInfo;
import kr.chapchap.place.domain.repository.UserTownRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserTownQueryService {

    private final UserTownRepository userTownRepository;

    public List<UserTownInfo> findRecentTowns(Long userId) {
        return userTownRepository.findByUserIdOrderBySelectedAtDesc(userId).stream()
                .map(UserTownInfo::from)
                .toList();
    }
}
