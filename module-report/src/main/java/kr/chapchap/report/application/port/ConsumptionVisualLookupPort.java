package kr.chapchap.report.application.port;

import kr.chapchap.report.application.info.ConsumptionVisualSummary;

import java.util.List;
import java.util.Map;

public interface ConsumptionVisualLookupPort {

    Map<Long, ConsumptionVisualSummary> findVisuals(Long userId, List<Long> consumptionIds);
}
