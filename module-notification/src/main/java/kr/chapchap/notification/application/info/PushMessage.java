package kr.chapchap.notification.application.info;

import java.util.Map;

public record PushMessage(String title, String body, Map<String, String> data) {
    public static PushMessage of(String title, String body) {
        return new PushMessage(title, body, Map.of());
    }

    public static record UserpushTarget (){
    }
}