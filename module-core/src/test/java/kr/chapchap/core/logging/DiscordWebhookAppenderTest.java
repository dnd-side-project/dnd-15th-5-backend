package kr.chapchap.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordWebhookAppenderTest {

    private HttpServer server;
    private final List<String> receivedBodies = new CopyOnWriteArrayList<>();
    private final List<String> receivedUserAgents = new CopyOnWriteArrayList<>();
    private DiscordWebhookAppender appender;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/webhook", exchange -> {
            try (InputStream body = exchange.getRequestBody()) {
                receivedBodies.add(new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }
            receivedUserAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        appender = new DiscordWebhookAppender();
        appender.setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/webhook");
        appender.setEnvironment("test");
        appender.setDedupeWindowSeconds(60);
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
    }

    @AfterEach
    void tearDown() {
        appender.stop();
        server.stop(0);
    }

    @Test
    void WARN_이상_로그는_Discord_Webhook으로_전송된다() {
        // when
        appender.doAppend(loggingEvent(Level.ERROR, "결제 처리 실패", new IllegalStateException("잔액 부족")));

        // then
        awaitReceivedBody(1);
        String payload = receivedBodies.get(0);
        assertThat(payload).contains("ERROR");
        assertThat(payload).contains("결제 처리 실패");
        assertThat(payload).contains("잔액 부족");
        assertThat(payload).contains("test");
    }

    @Test
    void 같은_로거와_메시지의_로그는_dedupe_기간_내에_한_번만_전송된다() {
        // given
        ILoggingEvent first = loggingEvent(Level.WARN, "외부 API 응답 지연", null);
        ILoggingEvent second = loggingEvent(Level.WARN, "외부 API 응답 지연", null);

        // when
        appender.doAppend(first);
        appender.doAppend(second);

        // then
        awaitReceivedBody(1);
        assertThat(receivedBodies).hasSize(1);
    }

    @Test
    void Discord_Webhook_요청에는_User_Agent_헤더가_포함된다() {
        // Discord는 User-Agent가 없는 요청을 403(error code 1010)으로 거부하는 경우가 있어 필수로 넣는다.
        // when
        appender.doAppend(loggingEvent(Level.ERROR, "외부 연동 실패", null));

        // then
        awaitReceivedBody(1);
        assertThat(receivedUserAgents.get(0)).isNotBlank();
    }

    @Test
    void MDC에_requestContext가_있으면_Request_필드로_알림에_포함된다() {
        // given
        ILoggingEvent event = loggingEvent(Level.ERROR, "장소 사진 조회 실패", null);
        ((LoggingEvent) event).setMDCPropertyMap(Map.of("requestContext", "GET /api/places/123/photos?lang=ko"));

        // when
        appender.doAppend(event);

        // then
        awaitReceivedBody(1);
        assertThat(receivedBodies.get(0)).contains("GET /api/places/123/photos?lang=ko");
    }

    @Test
    void MDC에_requestContext가_없으면_Request_필드가_없다() {
        // when
        appender.doAppend(loggingEvent(Level.ERROR, "요청과 무관한 배치 실패", null));

        // then
        awaitReceivedBody(1);
        assertThat(receivedBodies.get(0)).doesNotContain("\"name\":\"Request\"");
    }

    @Test
    void webhookUrl이_비어있으면_시작하지_않고_전송도_하지_않는다() {
        // given
        DiscordWebhookAppender blankAppender = new DiscordWebhookAppender();
        blankAppender.setWebhookUrl("");
        blankAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        blankAppender.start();

        // when
        blankAppender.doAppend(loggingEvent(Level.ERROR, "시작 전 호출", null));

        // then
        assertThat(blankAppender.isStarted()).isFalse();
    }

    private void awaitReceivedBody(int expectedSize) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3);
        while (receivedBodies.size() < expectedSize && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private ILoggingEvent loggingEvent(Level level, String message, Throwable throwable) {
        Logger logger = (Logger) LoggerFactory.getLogger("kr.chapchap.test");
        // loggerContext가 설정된 생성자를 써야 event.getMDCPropertyMap() 조회가 NPE 없이 동작한다.
        return new LoggingEvent(Logger.class.getName(), logger, level, message, throwable, null);
    }
}
