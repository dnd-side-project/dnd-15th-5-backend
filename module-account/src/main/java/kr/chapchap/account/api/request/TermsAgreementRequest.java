package kr.chapchap.account.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import kr.chapchap.account.application.command.TermsAgreementCommand;
import kr.chapchap.account.application.info.OAuthClientType;

@Schema(description = "필수 약관 동의 요청")
public record TermsAgreementRequest(
        @Schema(description = "서비스 이용약관 동의 여부", example = "true")
        @AssertTrue(message = "서비스 이용약관 동의는 필수입니다.")
        boolean serviceTermsAgreed,

        @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
        @AssertTrue(message = "개인정보 처리방침 동의는 필수입니다.")
        boolean privacyPolicyAgreed
) {

    public TermsAgreementCommand toCommand(
            Long userId,
            OAuthClientType clientType
    ) {
        return new TermsAgreementCommand(
                userId,
                clientType,
                serviceTermsAgreed,
                privacyPolicyAgreed
        );
    }
}
