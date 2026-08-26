package kr.chapchap.report.infra.external;

import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.report.application.port.UserNicknameLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserNicknameLookupAdapter implements UserNicknameLookupPort {

    private final AccountQueryService accountQueryService;

    @Override
    public Optional<String> findNickname(Long userId) {
        return accountQueryService.getNickname(userId);
    }
}
