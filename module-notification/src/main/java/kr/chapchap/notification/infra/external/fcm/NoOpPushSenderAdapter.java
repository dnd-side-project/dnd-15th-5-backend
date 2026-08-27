package kr.chapchap.notification.infra.external.fcm;

import kr.chapchap.notification.application.info.PushMessage;
import kr.chapchap.notification.application.info.PushSendResult;
import kr.chapchap.notification.application.port.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@ConditionalOnExpression("'${chapchap.notification.fcm.project-id:}'.trim().isEmpty()")
@Component
public class NoOpPushSenderAdapter implements PushSenderPort {

    @Override
    public PushSendResult sendMulticast(List<String> tokens, PushMessage message) {
        log.warn("FCM 미설정 상태라 푸시를 발송하지 않습니다(no-op). tokenCount={}, title={}", tokens.size(), message.title());
        return new PushSendResult(0, 0, List.of(), List.of());
    }

    @Override
    public String sendToTopic(String topic, PushMessage message) {
        log.warn("FCM 미설정 상태라 토픽 발송을 하지 않습니다(no-op). topic={}", topic);
        return null;
    }

    @Override
    public void subscribeToTopic(String token, String topic) {
        log.warn("FCM 미설정 상태라 토픽 구독을 하지 않습니다(no-op). topic={}", topic);
    }
}
