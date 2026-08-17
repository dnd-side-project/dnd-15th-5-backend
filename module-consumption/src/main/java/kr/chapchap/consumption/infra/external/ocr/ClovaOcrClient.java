package kr.chapchap.consumption.infra.external.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.chapchap.consumption.application.port.ReceiptOcrPort;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.consumption.infra.config.ClovaOcrProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class ClovaOcrClient implements ReceiptOcrPort {

    private static final String OCR_SECRET_HEADER = "X-OCR-SECRET";
    private static final String API_GATEWAY_KEY_HEADER = "x-ncp-apigw-api-key";
    private static final String OCR_VERSION = "V2";
    private static final String OCR_LANGUAGE = "ko";
    private static final String RECEIPT_IMAGE_NAME = "receipt";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";
    private static final String ERROR = "ERROR";

    private final RestClient restClient;
    private final ClovaOcrProperties properties;
    private final ClovaOcrRateLimiter rateLimiter;
    private final Clock clock;

    public ClovaOcrClient(
            @Qualifier("clovaOcrRestClient") RestClient restClient,
            ClovaOcrProperties properties,
            ClovaOcrRateLimiter rateLimiter,
            Clock clock
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Override
    public List<String> recognize(
            byte[] content,
            String contentType
    ) {
        validateRequest(content, contentType);

        String imageFormat = toImageFormat(contentType);
        String encodedContent = Base64.getEncoder().encodeToString(content);

        try {
            rateLimiter.awaitPermit();
            ClovaOcrRequest request = new ClovaOcrRequest(
                    OCR_VERSION,
                    UUID.randomUUID().toString(),
                    clock.millis(),
                    OCR_LANGUAGE,
                    List.of(new ClovaOcrRequestImage(
                            imageFormat,
                            RECEIPT_IMAGE_NAME,
                            encodedContent
                    ))
            );
            ClovaOcrResponse response = restClient.post()
                    .uri(properties.invokeUrl())
                    .header(OCR_SECRET_HEADER, properties.secretKey())
                    .header(API_GATEWAY_KEY_HEADER, properties.apiGatewayKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ClovaOcrResponse.class);
            return extractLines(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    exception
            );
        }
    }

    private void validateRequest(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("OCR 이미지 파일은 비어 있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("OCR 이미지 Content-Type은 비어 있을 수 없습니다.");
        }
    }

    private String toImageFormat(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new IllegalArgumentException("지원하지 않는 OCR 이미지 형식입니다.");
        };
    }

    private List<String> extractLines(ClovaOcrResponse response) {
        if (response == null || response.images() == null || response.images().size() != 1) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        ClovaOcrResponseImage image = response.images().getFirst();
        if (image == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        if (FAILURE.equals(image.inferResult())) {
            throw new BusinessException(
                    ConsumptionErrorCode.RECEIPT_OCR_RECOGNITION_FAILED
            );
        }
        if (ERROR.equals(image.inferResult()) || !SUCCESS.equals(image.inferResult())) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        return reconstructLines(image.fields());
    }

    private List<String> reconstructLines(List<ClovaOcrField> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (ClovaOcrField field : fields) {
            if (field == null) {
                continue;
            }
            if (field.inferText() != null && !field.inferText().isBlank()) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(' ');
                }
                currentLine.append(field.inferText().trim());
            }
            if (Boolean.TRUE.equals(field.lineBreak()) && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return List.copyOf(lines);
    }

    private record ClovaOcrRequest(
            String version,
            String requestId,
            long timestamp,
            String lang,
            List<ClovaOcrRequestImage> images
    ) {
    }

    private record ClovaOcrRequestImage(
            String format,
            String name,
            String data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClovaOcrResponse(List<ClovaOcrResponseImage> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClovaOcrResponseImage(
            String inferResult,
            List<ClovaOcrField> fields
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClovaOcrField(
            String inferText,
            Boolean lineBreak
    ) {
    }
}
