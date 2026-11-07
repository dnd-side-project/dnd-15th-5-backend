package kr.chapchap.report.application.service;

import kr.chapchap.report.application.command.GetReportCalendarCommand;
import kr.chapchap.report.application.info.ConsumptionVisualSummary;
import kr.chapchap.report.application.info.ReportCalendarInfo;
import kr.chapchap.report.application.info.ReportCalendarInfo.DayEntryInfo;
import kr.chapchap.report.application.info.ReportCalendarInfo.RecordDetailInfo;
import kr.chapchap.report.application.info.ReportCalendarRecord;
import kr.chapchap.report.application.port.ConsumptionCalendarLookupPort;
import kr.chapchap.report.application.port.ConsumptionVisualLookupPort;
import kr.chapchap.report.application.port.PlaceNameLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReportCalendarQueryService {

    private static final int DAYS_IN_WEEK = 7;

    private final ConsumptionCalendarLookupPort consumptionCalendarLookupPort;
    private final ConsumptionVisualLookupPort consumptionVisualLookupPort;
    private final PlaceNameLookupPort placeNameLookupPort;

    public ReportCalendarInfo getCalendar(GetReportCalendarCommand command) {
        LocalDate from = resolveRangeStart(command);
        LocalDate toExclusive = resolveRangeEndExclusive(command, from);

        List<ReportCalendarRecord> records = consumptionCalendarLookupPort.findByDateRange(command.userId(), from, toExclusive);

        if (records.isEmpty()) {
            return new ReportCalendarInfo(List.of());
        }

        List<Long> placeIds = records.stream().map(ReportCalendarRecord::placeId).distinct().toList();
        List<Long> consumptionIds = records.stream().map(ReportCalendarRecord::consumptionId).toList();

        Map<Long, Integer> visitOrdinals = consumptionCalendarLookupPort.findVisitOrdinals(command.userId(), placeIds, toExclusive);
        Map<Long, String> placeNames = placeNameLookupPort.findPlaceNames(placeIds);
        Map<Long, ConsumptionVisualSummary> visuals = consumptionVisualLookupPort.findVisuals(command.userId(), consumptionIds);

        Map<LocalDate, List<ReportCalendarRecord>> recordsByDate = records.stream()
                .collect(Collectors.groupingBy(ReportCalendarRecord::purchaseDate, LinkedHashMap::new, Collectors.toList()));

        List<DayEntryInfo> days = recordsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toDayEntryInfo(entry.getKey(), entry.getValue(), visitOrdinals, placeNames, visuals))
                .toList();

        return new ReportCalendarInfo(days);
    }

    private DayEntryInfo toDayEntryInfo(
            LocalDate date,
            List<ReportCalendarRecord> dayRecords,
            Map<Long, Integer> visitOrdinals,
            Map<Long, String> placeNames,
            Map<Long, ConsumptionVisualSummary> visuals
    ) {
        List<RecordDetailInfo> sortedRecords = dayRecords.stream()
                .sorted(Comparator.comparing(ReportCalendarRecord::purchaseTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(record -> toRecordDetailInfo(record, visitOrdinals, placeNames, visuals))
                .toList();
        return new DayEntryInfo(date, sortedRecords);
    }

    private RecordDetailInfo toRecordDetailInfo(
            ReportCalendarRecord record,
            Map<Long, Integer> visitOrdinals,
            Map<Long, String> placeNames,
            Map<Long, ConsumptionVisualSummary> visuals
    ) {
        ConsumptionVisualSummary visual = visuals.get(record.consumptionId());
        return new RecordDetailInfo(
                record.consumptionId(),
                record.placeId(),
                placeNames.get(record.placeId()),
                record.purchaseTime(),
                record.category(),
                record.amount(),
                visitOrdinals.getOrDefault(record.consumptionId(), 1),
                visual != null ? visual.stickerItemId() : null,
                visual != null ? visual.stickerName() : null,
                visual != null ? visual.imageId() : null,
                visual != null ? visual.imageUrl() : null
        );
    }

    private LocalDate resolveRangeStart(GetReportCalendarCommand command) {
        if (command.weekAnchor() != null) {
            return command.weekAnchor().minusDays(command.weekAnchor().getDayOfWeek().getValue() % DAYS_IN_WEEK);
        }
        return command.yearMonth().atDay(1);
    }

    private LocalDate resolveRangeEndExclusive(GetReportCalendarCommand command, LocalDate from) {
        if (command.weekAnchor() != null) {
            return from.plusDays(DAYS_IN_WEEK);
        }
        return command.yearMonth().plusMonths(1).atDay(1);
    }
}
