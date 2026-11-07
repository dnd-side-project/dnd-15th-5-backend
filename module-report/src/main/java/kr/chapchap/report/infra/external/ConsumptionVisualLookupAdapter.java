package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.info.ConsumptionVisualInfo;
import kr.chapchap.consumption.application.service.ConsumptionVisualBatchResolver;
import kr.chapchap.report.application.info.ConsumptionVisualSummary;
import kr.chapchap.report.application.port.ConsumptionVisualLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ConsumptionVisualLookupAdapter implements ConsumptionVisualLookupPort {

    private final ConsumptionVisualBatchResolver consumptionVisualBatchResolver;

    @Override
    public Map<Long, ConsumptionVisualSummary> findVisuals(Long userId, List<Long> consumptionIds) {
        Map<Long, ConsumptionVisualInfo> visuals =
                consumptionVisualBatchResolver.resolveByConsumptionIds(userId, consumptionIds);

        return visuals.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toSummary(entry.getValue())));
    }

    private ConsumptionVisualSummary toSummary(ConsumptionVisualInfo info) {
        return new ConsumptionVisualSummary(info.stickerItemId(), info.stickerName(), info.imageId(), info.imageUrl());
    }
}
