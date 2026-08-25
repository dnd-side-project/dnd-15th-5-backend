package kr.chapchap.account.application.service;

import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.consumption.application.service.ReceiptImageCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WithdrawnUserCleanupServiceTest {

    private static final Long FIRST_USER_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;

    @Mock
    private AccountWithdrawalService accountWithdrawalService;

    @Mock
    private ProfileImageStorage profileImageStorage;

    @Mock
    private ReceiptImageCleanupService receiptImageCleanupService;

    @InjectMocks
    private WithdrawnUserCleanupService withdrawnUserCleanupService;

    @Test
    void 탈퇴_회원의_파일을_삭제한_뒤_사용자를_삭제한다() {
        // given
        given(accountWithdrawalService.findWithdrawnUserIds())
                .willReturn(List.of(FIRST_USER_ID, SECOND_USER_ID));
        given(accountWithdrawalService.deleteWithdrawnUser(FIRST_USER_ID)).willReturn(true);
        given(accountWithdrawalService.deleteWithdrawnUser(SECOND_USER_ID)).willReturn(true);

        // when
        int result = withdrawnUserCleanupService.cleanupWithdrawnUsers();

        // then
        assertThat(result).isEqualTo(2);
        InOrder inOrder = inOrder(
                accountWithdrawalService,
                profileImageStorage,
                receiptImageCleanupService
        );
        inOrder.verify(accountWithdrawalService).findWithdrawnUserIds();
        inOrder.verify(profileImageStorage).deleteAllByUserId(FIRST_USER_ID);
        inOrder.verify(receiptImageCleanupService).deleteAllByUserId(FIRST_USER_ID);
        inOrder.verify(accountWithdrawalService).deleteWithdrawnUser(FIRST_USER_ID);
        inOrder.verify(profileImageStorage).deleteAllByUserId(SECOND_USER_ID);
        inOrder.verify(receiptImageCleanupService).deleteAllByUserId(SECOND_USER_ID);
        inOrder.verify(accountWithdrawalService).deleteWithdrawnUser(SECOND_USER_ID);
    }

    @Test
    void 한_회원의_파일_삭제가_실패해도_다른_회원은_계속_삭제한다() {
        // given
        given(accountWithdrawalService.findWithdrawnUserIds())
                .willReturn(List.of(FIRST_USER_ID, SECOND_USER_ID));
        willThrow(new IllegalStateException("S3 일시 오류"))
                .given(profileImageStorage)
                .deleteAllByUserId(FIRST_USER_ID);
        given(accountWithdrawalService.deleteWithdrawnUser(SECOND_USER_ID)).willReturn(true);

        // when
        int result = withdrawnUserCleanupService.cleanupWithdrawnUsers();

        // then
        assertThat(result).isOne();
        then(receiptImageCleanupService).should(never()).deleteAllByUserId(FIRST_USER_ID);
        then(accountWithdrawalService).should(never()).deleteWithdrawnUser(FIRST_USER_ID);
        then(profileImageStorage).should().deleteAllByUserId(SECOND_USER_ID);
        then(receiptImageCleanupService).should().deleteAllByUserId(SECOND_USER_ID);
        then(accountWithdrawalService).should().deleteWithdrawnUser(SECOND_USER_ID);
    }
}
