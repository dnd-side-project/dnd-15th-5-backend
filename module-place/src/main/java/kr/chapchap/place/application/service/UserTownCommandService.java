package kr.chapchap.place.application.service;

import kr.chapchap.place.application.command.UserTownSaveCommand;
import kr.chapchap.place.application.info.UserTownInfo;
import kr.chapchap.place.domain.entity.UserTown;
import kr.chapchap.place.domain.repository.UserTownRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.exception.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Transactional
@Service
public class UserTownCommandService {

    private static final int MAX_RECENT_TOWNS = 10;

    private final UserTownRepository userTownRepository;
    private final Clock clock;

    public UserTownInfo save(UserTownSaveCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserTown userTown = command.administrativeDongCode() == null
                ? userTownRepository.save(toNewUserTown(command, now))
                : findExistingOrCreate(command, now);

        enforceRecentLimit(command.userId());

        return UserTownInfo.from(userTown);
    }

    private UserTown findExistingOrCreate(UserTownSaveCommand command, LocalDateTime now) {
        return userTownRepository
                .findByUserIdAndAdministrativeDongCode(command.userId(), command.administrativeDongCode())
                .map(existing -> {
                    existing.touch(command.administrativeDongName(), command.latitude(), command.longitude(), now);
                    return existing;
                })
                .orElseGet(() -> userTownRepository.save(toNewUserTown(command, now)));
    }

    private UserTown toNewUserTown(UserTownSaveCommand command, LocalDateTime now) {
        return UserTown.create(
                command.userId(),
                command.administrativeDongCode(),
                command.administrativeDongName(),
                command.latitude(),
                command.longitude(),
                now
        );
    }

    public void delete(Long userId, Long userTownId) {
        UserTown userTown = userTownRepository.findByIdAndUserId(userTownId, userId)
                .orElseThrow(() -> new BusinessException(PlaceErrorCode.USER_TOWN_NOT_FOUND));
        userTownRepository.delete(userTown);
    }

    private void enforceRecentLimit(Long userId) {
        List<UserTown> towns = userTownRepository.findByUserIdOrderBySelectedAtDesc(userId);
        if (towns.size() <= MAX_RECENT_TOWNS) {
            return;
        }
        List<UserTown> overflow = towns.subList(MAX_RECENT_TOWNS, towns.size());
        userTownRepository.deleteAll(overflow);
    }
}
