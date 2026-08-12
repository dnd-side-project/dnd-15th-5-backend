package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.MonthlyAggregationResult;
import kr.chapchap.report.domain.entity.MonthlyVisitActivity;
import kr.chapchap.report.domain.entity.PersonaScoreResult;

import java.util.List;
import java.util.Set;

public interface PersonaScoringService {

    PersonaScoreResult score(
            List<MonthlyVisitActivity> monthActivities,
            Set<Long> priorVisitedPlaceIds,
            MonthlyAggregationResult aggregationResult
    );
}
