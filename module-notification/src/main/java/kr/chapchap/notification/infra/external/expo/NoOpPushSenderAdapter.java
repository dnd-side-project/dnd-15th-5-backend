
package kr.chapchap.notification.infra.external.expo;

import kr.chapchap.notification.application.info.PushMessage;
import kr.chapchap.notification.application.info.PushSendResult;
import kr.chapchap.notification.application.port.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@ConditionalOnProperty(prefix = "chapchap.notification.expo", name = "enabled", havingValue = "false", matchIfMissing = true)
@Component
public class NoOpPushSenderAdapter implements PushSenderPort {

    @Override
    public PushSendResult sendMulticast(List<String> tokens, PushMessage message) {
        log.warn("Expo Push 미설정 상태라 푸시를 발송하지 않습니다(no-op). tokenCount={}, title={}", tokens.size(), message.title());
        return new PushSendResult(0, 0, List.of(), List.of());
    }
}
