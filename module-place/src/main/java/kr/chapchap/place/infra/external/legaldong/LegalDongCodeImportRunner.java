package kr.chapchap.place.infra.external.legaldong;

import kr.chapchap.place.application.service.AdministrativeDongReplaceService;
import kr.chapchap.place.domain.entity.AdministrativeDong;
import kr.chapchap.place.infra.external.legaldong.LegalDongCodeApiClient.LegalDongCodePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Profile("legal-dong-import")
@Component
public class LegalDongCodeImportRunner implements ApplicationRunner {

    private static final int PAGE_SIZE = 1000;

    private final LegalDongCodeApiClient legalDongCodeApiClient;
    private final AdministrativeDongReplaceService administrativeDongReplaceService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("법정동코드 전체자료 적재를 시작");

        List<AdministrativeDong> toSave = new ArrayList<>();
        int pageNo = 1;
        long totalCount = Long.MAX_VALUE;
        long fetched = 0;

        while ((pageNo - 1L) * PAGE_SIZE < totalCount) {
            LegalDongCodePage page = legalDongCodeApiClient.fetchPage(pageNo, PAGE_SIZE);
            totalCount = page.totalCount() > 0 ? page.totalCount() : 0;
            if (page.rows().isEmpty()) {
                break;
            }
            fetched += page.rows().size();

            page.rows().stream()
                    .filter(row -> !row.isAbolished())
                    .filter(LegalDongCodeRow::isDongLevel)
                    .map(this::toEntity)
                    .filter(entity -> entity != null)
                    .forEach(toSave::add);

            log.info("법정동코드 {}페이지 수신 (누적 수신 {}건 / 전체 {}건, 동 단위로 필터링된 누적 {}건)",
                    pageNo, fetched, totalCount, toSave.size());
            pageNo++;
        }

        if (toSave.isEmpty()) {
            log.warn("법정동코드 적재 결과가 0건입니다");
            return;
        }

        administrativeDongReplaceService.replaceAll(toSave);
        log.info("법정동코드 적재 완료: {}건 저장.", toSave.size());
    }

    private AdministrativeDong toEntity(LegalDongCodeRow row) {
        LegalDongNameParser.ParsedName parsed = LegalDongNameParser.parse(row.locataddNm());
        if (parsed == null || row.regionCd() == null) {
            return null;
        }
        return AdministrativeDong.create(row.regionCd(), parsed.sidoName(), parsed.sigunguName(), parsed.dongName());
    }
}
