package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import kr.chapchap.account.api.response.AuthenticationResponse;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.LoginTokenService;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dev Authentication", description = "로컬·개발 환경 전용 테스트 인증 API")
@Profile("local | dev")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth/dev")
public class DevAuthenticationController {

    private final LoginTokenService loginTokenService;
    private final AuthenticationResponseHandler authenticationResponseHandler;

    @Operation(
            summary = "테스트 토큰 발급",
            description = """
                    기존 ACTIVE 사용자의 토큰을 소셜 로그인 없이 발급합니다. 사용자를 생성하거나 상태를 변경하지 않습니다.
                    local 또는 dev 프로필에서만 제공하며, Dev 배포는 prod,dev 프로필을 사용합니다.
                    응답의 accessToken을 Swagger Authorize에 입력하세요. Refresh Token은 APP 방식으로 본문에 반환합니다.
                    """
    )
    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> issueToken(
            @Parameter(description = "개발 DB에 존재하는 ACTIVE 사용자 ID (로컬 시드: 1, 2, 3)")
            @RequestParam(defaultValue = "1") Long userId,
            HttpServletResponse response
    ) {
        AuthenticationInfo info = loginTokenService.issueForActiveUser(userId, OAuthClientType.APP);
        return ApiResponse.success(authenticationResponseHandler.handle(info, response));
    }
}
