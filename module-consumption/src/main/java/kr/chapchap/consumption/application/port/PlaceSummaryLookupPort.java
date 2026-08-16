package kr.chapchap.consumption.application.port;

import kr.chapchap.consumption.application.info.PlaceSummaryInfo;

import java.util.List;
import java.util.Map;


public interface PlaceSummaryLookupPort {

    Map<Long, PlaceSummaryInfo> findSummaries(List<Long> placeIds);
}
