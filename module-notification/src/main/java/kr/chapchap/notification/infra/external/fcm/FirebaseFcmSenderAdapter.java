package kr.chapchap.notification.infra.external.fcm;

import com.google.firebase.messaging.*;
import kr.chapchap.notification.application.port.PushMessage;
import kr.chapchap.notification.application.port.PushSendResult;
import kr.chapchap.notification.application.port.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class FirebaseFcmSenderAdapter implements PushSenderPort {
    private static final int MAX_TOKENS_PER_REQUEST = 500;

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult sendMulticast(List<String> tokens, PushMessage message) {
        int successCount = 0;
        int failureCount = 0;
        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i += MAX_TOKENS_PER_REQUEST) {
            List<String> chunk = tokens.subList(i, Math.min(i + MAX_TOKENS_PER_REQUEST, tokens.size()));
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(toFcmNotification(message))
                    .putAllData(message.data())
                    .setApnsConfig(toApnsConfig())
                    .build();

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();

                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse sendResponse = responses.get(j);
                    if (sendResponse.isSuccessful()) {
                        continue;
                    }
                    MessagingErrorCode errorCode = sendResponse.getException() != null
                            ? sendResponse.getException().getMessagingErrorCode()
                            : null;
                    if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        invalidTokens.add(chunk.get(j));
                    }
                }
            } catch (FirebaseMessagingException exception) {
                log.error("FCM 멀티캐스트 발송 실패. size={}", chunk.size(), exception);
                failureCount += chunk.size();
            }
        }

        return new PushSendResult(successCount, failureCount, invalidTokens);
    }

    @Override
    public String sendToTopic(String topic, PushMessage message) {
        Message fcmMessage = Message.builder()
                .setTopic(topic)
                .setNotification(toFcmNotification(message))
                .putAllData(message.data())
                .setApnsConfig(toApnsConfig())
                .build();
        try {
            return firebaseMessaging.send(fcmMessage);
        } catch (FirebaseMessagingException exception) {
            log.error("FCM 토픽 발송 실패. topic={}", topic, exception);
            throw new IllegalStateException("토픽 발송에 실패했습니다. topic=" + topic, exception);
        }
    }

    @Override
    public void subscribeToTopic(String token, String topic) {
        try {
            firebaseMessaging.subscribeToTopic(List.of(token), topic);
        } catch (FirebaseMessagingException exception) {
            log.error("FCM 토픽 구독 실패. topic={}", topic, exception);
        }
    }

    private Notification toFcmNotification(PushMessage message) {
        return Notification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .build();
    }

    private ApnsConfig toApnsConfig() {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setSound("default").build())
                .build();
    }
}