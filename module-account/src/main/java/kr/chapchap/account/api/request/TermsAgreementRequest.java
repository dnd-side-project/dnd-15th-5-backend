package kr.chapchap.account.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import kr.chapchap.account.application.command.TermsAgreementCommand;
import kr.chapchap.account.application.info.OAuthClientType;

@Schema(description = "가입 자격 확인 및 서비스 이용약관 동의 요청")
public record TermsAgreementRequest(
        @Schema(
                description = "만 14세 이상 확인 여부",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @AssertTrue(message = "만 14세 이상 확인은 필수입니다.")
        boolean ageConfirmed,

        @Schema(
                description = "서비스 이용약관 동의 여부",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @AssertTrue(message = "서비스 이용약관 동의는 필수입니다.")
        boolean serviceTermsAgreed
) {

    public TermsAgreementCommand toCommand(
            Long userId,
            OAuthClientType clientType
    ) {
        return new TermsAgreementCommand(
                userId,
                clientType,
                serviceTermsAgreed
        );
    }
}
