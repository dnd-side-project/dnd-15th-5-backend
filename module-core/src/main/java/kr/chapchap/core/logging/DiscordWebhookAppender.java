package kr.chapchap.core.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class DiscordWebhookAppender extends AppenderBase<ILoggingEvent> {

    private static final int DESCRIPTION_MAX_LENGTH = 1800;
    private static final int STACK_TRACE_MAX_LENGTH = 1000;
    private static final int ERROR_COLOR = 15158332; // #E74C3C
    private static final int WARN_COLOR = 15844367;  // #F1C40F
    private static final String USER_AGENT = "ChapChap-Backend-Alert/1.0";
    private static final String REQUEST_CONTEXT_MDC_KEY = "requestContext";

    private String webhookUrl;
    private String applicationName = "ChapChap Backend";
    private String environment = "unknown";
    private long dedupeWindowSeconds = 60;

    private HttpClient httpClient;
    private ExecutorService executor;
    private final ConcurrentHashMap<String, Instant> lastSentAt = new ConcurrentHashMap<>();

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setDedupeWindowSeconds(long dedupeWindowSeconds) {
        this.dedupeWindowSeconds = dedupeWindowSeconds;
    }

    @Override
    public void start() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            addWarn("webhookUrl이 비어 있어 DiscordWebhookAppender를 시작하지 않습니다.");
            return;
        }
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "discord-webhook-appender");
            thread.setDaemon(true);
            return thread;
        });
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || isDuplicateWithinWindow(event)) {
            return;
        }
        String payload = buildPayload(event);
        executor.submit(() -> send(payload));
    }

    private boolean isDuplicateWithinWindow(ILoggingEvent event) {
        String key = event.getLevel() + "|" + event.getLoggerName() + "|" + event.getFormattedMessage();
        Instant now = Instant.now();
        Instant previous = lastSentAt.put(key, now);
        return previous != null && previous.plusSeconds(dedupeWindowSeconds).isAfter(now);
    }

    private void send(String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                addWarn("Discord Webhook 응답 코드: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            addWarn("Discord Webhook 전송 실패", e);
        }
    }

    private String buildPayload(ILoggingEvent event) {
        boolean isError = event.getLevel().toInt() >= ch.qos.logback.classic.Level.ERROR_INT;
        String title = isError ? "🚨 ERROR" : "⚠️ WARN";
        int color = isError ? ERROR_COLOR : WARN_COLOR;

        StringBuilder description = new StringBuilder();
        description.append("```\n").append(truncate(event.getFormattedMessage(), DESCRIPTION_MAX_LENGTH)).append("\n```");

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            String stackTrace = truncate(ThrowableProxyUtil.asString(throwableProxy), STACK_TRACE_MAX_LENGTH);
            description.append("\n```\n").append(stackTrace).append("\n```");
        }

        StringBuilder fields = new StringBuilder();
        fields.append(field("Logger", event.getLoggerName()))
                .append(",")
                .append(field("Thread", event.getThreadName()))
                .append(",")
                .append(field("Environment", environment));

        String requestContext = event.getMDCPropertyMap().get(REQUEST_CONTEXT_MDC_KEY);
        if (requestContext != null && !requestContext.isBlank()) {
            fields.append(",").append(fieldFullWidth("Request", requestContext));
        }

        return "{"
                + "\"username\":\"" + escape(applicationName + " Alert") + "\","
                + "\"embeds\":[{"
                + "\"title\":\"" + escape(title) + "\","
                + "\"description\":\"" + escape(description.toString()) + "\","
                + "\"color\":" + color + ","
                + "\"fields\":[" + fields + "],"
                + "\"timestamp\":\"" + Instant.ofEpochMilli(event.getTimeStamp()) + "\""
                + "}]"
                + "}";
    }

    private String field(String name, String value) {
        return "{\"name\":\"" + escape(name) + "\",\"value\":\"" + escape(value) + "\",\"inline\":true}";
    }

    private String fieldFullWidth(String name, String value) {
        return "{\"name\":\"" + escape(name) + "\",\"value\":\"" + escape(value) + "\",\"inline\":false}";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...(truncated)";
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
