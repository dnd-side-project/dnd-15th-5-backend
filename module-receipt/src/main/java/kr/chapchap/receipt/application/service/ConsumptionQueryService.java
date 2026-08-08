package kr.chapchap.receipt.application.service;

import kr.chapchap.receipt.application.command.ConsumptionSearchCommand;
import kr.chapchap.receipt.application.info.ConsumptionInfo;
import kr.chapchap.receipt.application.info.ConsumptionScrollInfo;
import kr.chapchap.receipt.application.port.PlaceNameLookupPort;
import kr.chapchap.receipt.domain.entity.Consumption;
import kr.chapchap.receipt.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ConsumptionQueryService {

    private static final String UNKNOWN_PLACE_NAME = "알 수 없는 가게";

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final PlaceNameLookupPort placeNameLookupPort;

    public ConsumptionScrollInfo search(ConsumptionSearchCommand command) {
        YearMonth yearMonth = command.yearMonth();
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEndExclusive = yearMonth.plusMonths(1).atDay(1);

        int fetchSize = command.size() + 1;
        List<Consumption> fetched = consumptionQueryRepository.searchByCursor(
                command.userId(),
                monthStart,
                monthEndExclusive,
                command.cursorPurchaseDate(),
                command.cursorPurchaseTime(),
                command.cursorId(),
                fetchSize
        );

        boolean hasNext = fetched.size() > command.size();
        List<Consumption> content = hasNext ? fetched.subList(0, command.size()) : fetched;

        List<Long> placeIds = content.stream().map(Consumption::getPlaceId).distinct().toList();
        Map<Long, String> placeNames = placeNameLookupPort.findNames(placeIds);

        List<ConsumptionInfo> consumptions = content.stream()
                .map(consumption -> ConsumptionInfo.of(
                        consumption,
                        placeNames.getOrDefault(consumption.getPlaceId(), UNKNOWN_PLACE_NAME)
                ))
                .toList();

        Consumption last = content.isEmpty() ? null : content.get(content.size() - 1);

        return new ConsumptionScrollInfo(
                consumptions,
                hasNext,
                last != null ? last.getPurchaseDate() : null,
                last != null ? last.getPurchaseTime() : null,
                last != null ? last.getId() : null
        );
    }
}
