package kr.chapchap.place.infra.external.legaldong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.infra.config.LegalDongCodeProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class LegalDongCodeApiClient {

    private static final String SERVICE_PATH = "/1741000/StanReginCd/getStanReginCdList";

    private final RestClient restClient;
    private final LegalDongCodeProperties properties;
    private final ObjectMapper objectMapper;

    public LegalDongCodeApiClient(
            @Qualifier("legalDongCodeRestClient") RestClient restClient,
            LegalDongCodeProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public LegalDongCodePage fetchPage(int pageNo, int numOfRows) {
        URI uri = UriComponentsBuilder.fromUri(properties.baseUri())
                .path(SERVICE_PATH)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .build()
                .encode()
                .toUri();

        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            JsonNode response = objectMapper.readTree(body);
            return parse(response);
        } catch (RestClientException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private LegalDongCodePage parse(JsonNode response) {
        if (response == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        JsonNode rowsNode = findArrayNode(response, "row");
        long totalCount = findTotalCount(response);

        List<LegalDongCodeRow> rows = new ArrayList<>();
        if (rowsNode != null) {
            for (JsonNode row : rowsNode) {
                rows.add(new LegalDongCodeRow(
                        textOf(row, "region_cd"),
                        textOf(row, "sido_cd"),
                        textOf(row, "sgg_cd"),
                        textOf(row, "umd_cd"),
                        textOf(row, "ri_cd"),
                        textOf(row, "locatadd_nm"),
                        textOf(row, "locat_rm")
                ));
            }
        }

        return new LegalDongCodePage(rows, totalCount);
    }


    private JsonNode findArrayNode(JsonNode node, String fieldName) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findArrayNode(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isObject()) {
            if (node.has(fieldName) && node.get(fieldName).isArray()) {
                return node.get(fieldName);
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode found = findArrayNode(fields.next().getValue(), fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private long findTotalCount(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                long found = findTotalCount(child);
                if (found >= 0) {
                    return found;
                }
            }
        } else if (node.isObject()) {
            if (node.has("totalCount")) {
                return node.get("totalCount").asLong();
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                long found = findTotalCount(fields.next().getValue());
                if (found >= 0) {
                    return found;
                }
            }
        }
        return -1;
    }

    private String textOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public record LegalDongCodePage(List<LegalDongCodeRow> rows, long totalCount) {
    }
}
