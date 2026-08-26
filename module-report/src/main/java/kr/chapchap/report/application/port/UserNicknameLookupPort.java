package kr.chapchap.report.application.port;

import java.util.Optional;

public interface UserNicknameLookupPort {

    Optional<String> findNickname(Long userId);
}
