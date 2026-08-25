package kr.chapchap.account.application.service;

import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.consumption.application.service.ReceiptImageCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class WithdrawnUserCleanupService {

    private final AccountWithdrawalService accountWithdrawalService;
    private final ProfileImageStorage profileImageStorage;
    private final ReceiptImageCleanupService receiptImageCleanupService;

    public int cleanupWithdrawnUsers() {
        List<Long> userIds = accountWithdrawalService.findWithdrawnUserIds();
        int deletedCount = 0;
        int failedCount = 0;

        for (Long userId : userIds) {
            try {
                profileImageStorage.deleteAllByUserId(userId);
                receiptImageCleanupService.deleteAllByUserId(userId);
                if (accountWithdrawalService.deleteWithdrawnUser(userId)) {
                    deletedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                log.error(
                        "탈퇴 회원 데이터 삭제에 실패했습니다.",
                        exception
                );
            }
        }

        log.info(
                "탈퇴 회원 데이터 삭제를 완료했습니다. targetCount={}, deletedCount={}, failedCount={}",
                userIds.size(),
                deletedCount,
                failedCount
        );
        return deletedCount;
    }
}
