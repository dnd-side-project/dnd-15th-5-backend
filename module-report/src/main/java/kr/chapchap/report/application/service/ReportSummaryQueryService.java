package kr.chapchap.report.application.service;

import kr.chapchap.report.application.info.SummaryCandidateInfo;
import kr.chapchap.report.application.info.SummaryEvaluationResult;
import kr.chapchap.report.application.info.SummaryHighlightInfo;
import kr.chapchap.report.domain.entity.ReportSummaryExposure;
import kr.chapchap.report.domain.entity.SummaryHighlightType;
import kr.chapchap.report.domain.repository.ReportSummaryExposureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
@Transactional
@Service
public class ReportSummaryQueryService {

    private final ReportSummaryCandidateEvaluator candidateEvaluator;
    private final ReportSummaryExposureRepository exposureRepository;
    private final Clock clock;

    public SummaryHighlightInfo getSummaryHighlight(Long userId) {
        LocalDate today = LocalDate.now(clock);
        SummaryEvaluationResult result = candidateEvaluator.evaluate(userId);

        if (result.totalEverCount() == 0) {
            return fallback("첫 소비 기록을 남겨보세요!", "기록이 쌓이면 나만의 방문 패턴을 알려드릴게요.");
        }
        if (result.totalEverCount() < 5) {
            return fallback(
                    "나만의 소비 지도를 채워가고 있어요!",
                    "지금까지 %d개의 기록을 남겼어요.".formatted(result.totalEverCount()));
        }
        if (result.candidates().isEmpty()) {
            return fallback(
                    "다양한 곳에 발자국을 남겨보세요!",
                    "최근 3개월 동안 %d곳의 가게를 기록했어요.".formatted(result.recentThreeMonthsDistinctPlaceCount()));
        }

        SummaryCandidateInfo selected = selectCandidate(userId, today, result.candidates());
        return new SummaryHighlightInfo(selected.type(), selected.mainCopy(), selected.subCopy());
    }

    private SummaryCandidateInfo selectCandidate(Long userId, LocalDate today, List<SummaryCandidateInfo> candidates) {
        Optional<ReportSummaryExposure> todayExposure = exposureRepository.findByUserIdAndExposureDate(userId, today);

        if (todayExposure.isPresent()) {
            Optional<SummaryCandidateInfo> stillValid = candidates.stream()
                    .filter(candidate -> candidate.type() == todayExposure.get().getSelectedType())
                    .findFirst();
            if (stillValid.isPresent()) {
                return stillValid.get();
            }
        }

        List<SummaryHighlightType> recentlyShown = exposureRepository.findTop7ByUserIdOrderByExposureDateDesc(userId).stream()
                .map(ReportSummaryExposure::getSelectedType)
                .toList();

        SummaryCandidateInfo selected = candidates.stream()
                .filter(candidate -> !recentlyShown.contains(candidate.type()))
                .findFirst()
                .orElseGet(() -> candidates.get(0)); // 후보 전부 노출했으면 새로 순환 시작

        saveExposure(userId, today, selected.type(), todayExposure);
        return selected;
    }

    private void saveExposure(Long userId, LocalDate today, SummaryHighlightType type, Optional<ReportSummaryExposure> existing) {
        existing.ifPresent(exposureRepository::delete);
        exposureRepository.save(ReportSummaryExposure.create(userId, today, type));
    }

    private SummaryHighlightInfo fallback(String mainCopy, String subCopy) {
        return new SummaryHighlightInfo(null, mainCopy, subCopy);
    }
}
