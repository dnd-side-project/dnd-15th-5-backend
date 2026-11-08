package kr.chapchap.report.application.service;

import kr.chapchap.report.application.info.ConsumptionVisualSummary;
import kr.chapchap.report.application.info.ReportCalendarRecord;
import kr.chapchap.report.application.info.StickerBoardInfo;
import kr.chapchap.report.application.info.StickerBoardInfo.DayVisualsInfo;
import kr.chapchap.report.application.info.StickerBoardInfo.StickerBoardVisualInfo;
import kr.chapchap.report.application.port.ConsumptionCalendarLookupPort;
import kr.chapchap.report.application.port.ConsumptionVisualLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReportStickerBoardQueryService {

    private final ConsumptionCalendarLookupPort consumptionCalendarLookupPort;
    private final ConsumptionVisualLookupPort consumptionVisualLookupPort;

    public StickerBoardInfo getStickerBoard(Long userId, YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        LocalDate toExclusive = yearMonth.plusMonths(1).atDay(1);

        List<ReportCalendarRecord> records = consumptionCalendarLookupPort.findByDateRange(userId, from, toExclusive);

        if (records.isEmpty()) {
            return new StickerBoardInfo(yearMonth, List.of());
        }

        List<Long> consumptionIds = records.stream().map(ReportCalendarRecord::consumptionId).toList();
        Map<Long, ConsumptionVisualSummary> visuals = consumptionVisualLookupPort.findVisuals(userId, consumptionIds);

        Map<LocalDate, List<ReportCalendarRecord>> recordsByDate = records.stream()
                .collect(Collectors.groupingBy(ReportCalendarRecord::purchaseDate, LinkedHashMap::new, Collectors.toList()));

        List<DayVisualsInfo> days = recordsByDate.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<ReportCalendarRecord>>comparingByKey().reversed())
                .map(entry -> toDayVisualsInfo(entry.getKey(), entry.getValue(), visuals))
                .toList();

        return new StickerBoardInfo(yearMonth, days);
    }

    private DayVisualsInfo toDayVisualsInfo(LocalDate date, List<ReportCalendarRecord> dayRecords, Map<Long, ConsumptionVisualSummary> visuals
    ) {
        List<StickerBoardVisualInfo> items = dayRecords.stream()
                .sorted(Comparator.comparing(ReportCalendarRecord::purchaseTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(record -> toVisualInfo(record, visuals))
                .toList();
        return new DayVisualsInfo(date, items);
    }

    private StickerBoardVisualInfo toVisualInfo(ReportCalendarRecord record, Map<Long, ConsumptionVisualSummary> visuals) {
        ConsumptionVisualSummary visual = visuals.get(record.consumptionId());
        return new StickerBoardVisualInfo(
                record.consumptionId(),
                record.category(),
                visual != null ? visual.stickerItemId() : null,
                visual != null ? visual.stickerName() : null,
                visual != null ? visual.imageId() : null,
                visual != null ? visual.imageUrl() : null
        );
    }
}
