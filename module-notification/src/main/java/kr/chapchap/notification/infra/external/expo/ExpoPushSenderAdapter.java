package kr.chapchap.notification.infra.external.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.chapchap.notification.application.info.PushMessage;
import kr.chapchap.notification.application.info.PushSendResult;
import kr.chapchap.notification.application.port.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@ConditionalOnProperty(prefix = "chapchap.notification.expo", name = "enabled", havingValue = "true")
@Component
public class ExpoPushSenderAdapter implements PushSenderPort {

    private static final int MAX_TOKENS_PER_REQUEST = 100;
    private static final String TICKET_STATUS_OK = "ok";
    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    private final RestClient restClient;

    public ExpoPushSenderAdapter(@Qualifier("expoRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PushSendResult sendMulticast(List<String> tokens, PushMessage message) {
        int successCount = 0;
        int failureCount = 0;
        List<String> invalidTokens = new ArrayList<>();
        List<String> failedTokens = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i += MAX_TOKENS_PER_REQUEST) {
            List<String> chunk = tokens.subList(i, Math.min(i + MAX_TOKENS_PER_REQUEST, tokens.size()));
            List<ExpoPushMessageRequest> requestBody = chunk.stream()
                    .map(token -> toExpoPushMessageRequest(token, message))
                    .toList();

            try {
                ExpoPushResponse response = restClient.post()
                        .body(requestBody)
                        .retrieve()
                        .body(ExpoPushResponse.class);

                List<ExpoPushTicket> tickets = response != null ? response.data() : null;

                if (tickets == null || tickets.size() != chunk.size()) {
                    log.error("Expo 푸시 발송 응답이 유효하지 않습니다. startIndex={}, chunkSize={}, ticketSize={}", i, chunk.size(), tickets == null ? null : tickets.size());
                    failureCount += chunk.size();
                    failedTokens.addAll(chunk);
                    continue;
                }

                for (int j = 0; j < tickets.size(); j++) {
                    ExpoPushTicket ticket = tickets.get(j);
                    String token = chunk.get(j);
                    if (TICKET_STATUS_OK.equals(ticket.status())) {
                        successCount++;
                        continue;
                    }
                    failureCount++;
                    String errorCode = ticket.details() != null ? ticket.details().error() : null;
                    if (DEVICE_NOT_REGISTERED.equals(errorCode)) {
                        invalidTokens.add(token);
                    } else {
                        failedTokens.add(token);
                    }
                }
            } catch (RestClientException exception) {
                log.error("Expo 푸시 발송 실패. size={}", chunk.size(), exception);
                failureCount += chunk.size();
                failedTokens.addAll(chunk);
            }
        }

        return new PushSendResult(successCount, failureCount, invalidTokens, failedTokens);
    }

    private ExpoPushMessageRequest toExpoPushMessageRequest(String token, PushMessage message) {
        return new ExpoPushMessageRequest(token, message.title(), message.body(), message.data(), "default", "high");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpoPushMessageRequest(
            String to,
            String title,
            String body,
            Map<String, String> data,
            String sound,
            String priority
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpoPushResponse(List<ExpoPushTicket> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpoPushTicket(String status, String id, String message, ExpoPushTicketDetails details) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpoPushTicketDetails(String error) {
    }
}
