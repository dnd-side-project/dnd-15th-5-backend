package kr.chapchap.account.application.info;

import java.net.URI;

public record AccountWithdrawalCallbackInfo(
        URI redirectUri,
        boolean completed
) {
}
