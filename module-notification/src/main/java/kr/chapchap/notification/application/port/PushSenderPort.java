package kr.chapchap.notification.application.port;

import java.util.List;

public interface PushSenderPort {
    PushSendResult sendMulticast(List<String> tokens, PushMessage message);

    String sendToTopic(String topic, PushMessage message);
    void subscribeToTopic(String token, String topic);
}
