package kr.chapchap.report.application.port;

import java.util.List;
import java.util.Map;

public interface DongNameLookupPort {

    Map<Long, String> findDongNames(List<Long> placeIds);
}
