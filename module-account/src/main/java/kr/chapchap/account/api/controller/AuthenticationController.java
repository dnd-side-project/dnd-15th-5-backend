package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.chapchap.account.api.request.LoginCodeExchangeRequest;
import kr.chapchap.account.api.request.RefreshTokenRequest;
import kr.chapchap.account.api.request.TermsAgreementRequest;
import kr.chapchap.account.api.response.AuthenticationResponse;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.LoginTokenService;
import kr.chapchap.account.application.service.OAuthFlowService;
import kr.chapchap.account.application.service.TermsAgreementService;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kr.chapchap.account.api.response.AuthenticationResponseHandler.REFRESH_TOKEN_COOKIE_NAME;

@Tag(name = "Authentication", description = "소셜 로그인, 회원가입 완료 및 토큰 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final OAuthFlowService oauthFlowService;
    private final TermsAgreementService termsAgreementService;
    private final LoginTokenService loginTokenService;
    private final AuthenticationResponseHandler authenticationResponseHandler;

    @Operation(
            summary = "소셜 로그인 코드 교환",
            description = """
                    소셜 로그인 콜백이 전달한 일회용 loginCode와 로그인 시작 시 생성한 codeVerifier를 교환합니다.

                    가입이 완료된 사용자는 Access Token을 발급하고, 약관 동의가 필요한 사용자는 Signup Token을 발급합니다.

                    WEB의 Refresh Token은 HttpOnly 쿠키로, APP의 Refresh Token은 응답 본문으로 전달합니다.

                    Authorization 헤더 없이 호출합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 완료 또는 필수 약관 동의 필요",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "WEB 로그인 완료 시 발급되는 refresh_token 쿠키"
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (C001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "loginCode가 만료되었거나 PKCE 검증에 실패함 (C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "로그인할 수 없는 계정 상태 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/social/exchange")
    public ApiResponse<AuthenticationResponse> exchangeSocialLoginCode(
            @Valid @RequestBody LoginCodeExchangeRequest request,
            HttpServletResponse response
    ) {
        AuthenticationInfo info = oauthFlowService.exchange(
                request.loginCode(),
                request.codeVerifier()
        );
        return ApiResponse.success(
                authenticationResponseHandler.handle(info, response)
        );
    }

    @Operation(
            summary = "서비스 이용약관 동의 및 회원가입 완료",
            description = """
                    로그인 코드 교환에서 발급된 Signup Token을 Bearer 토큰으로 사용합니다.

                    서비스 이용약관에 동의하면 회원가입을 완료하고 Access Token과 Refresh Token을 발급합니다.

                    Refresh Token 전달 방식은 로그인한 WEB/APP 유형을 따릅니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 완료 및 사용자 토큰 발급",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "WEB 회원가입 완료 시 발급되는 refresh_token 쿠키"
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "서비스 이용약관 미동의 또는 요청 값 검증 실패 (C001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Signup Token이 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Signup Token 권한이 아니거나 이미 가입이 완료된 사용자 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/signup/terms")
    public ApiResponse<AuthenticationResponse> agree(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TermsAgreementRequest request,
            HttpServletResponse response
    ) {
        AuthenticationInfo info = termsAgreementService.agree(
                request.toCommand(
                        Long.valueOf(jwt.getSubject()),
                        OAuthClientType.fromClaim(jwt.getClaimAsString("client_type"))
                )
        );
        return ApiResponse.success(
                authenticationResponseHandler.handle(info, response)
        );
    }

    @Operation(
            summary = "앱 토큰 재발급",
            description = """
                    APP Refresh Token을 요청 본문으로 받아 기존 토큰을 폐기하고 새 토큰 쌍을 발급합니다.

                    발급된 Access Token과 Refresh Token은 모두 응답 본문으로 전달합니다.

                    Authorization 헤더 없이 호출합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "APP 토큰 재발급 및 회전 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (C001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 만료되었거나 유효하지 않음 (C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "토큰을 재발급할 수 없는 계정 상태 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/token/refresh")
    public ApiResponse<AuthenticationResponse> refreshApp(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse response
    ) {
        AuthenticationInfo info = loginTokenService.refresh(
                request.refreshToken(),
                OAuthClientType.APP
        );
        return ApiResponse.success(
                authenticationResponseHandler.handle(info, response)
        );
    }

    @Operation(
            summary = "앱 로그아웃",
            description = """
                    APP Refresh Token을 요청 본문으로 받아 서버에 저장된 토큰을 폐기합니다.

                    앱은 응답을 받은 뒤 로컬에 저장한 Access Token과 Refresh Token을 삭제합니다.

                    Authorization 헤더 없이 호출합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "APP 로그아웃 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (C001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 만료되었거나 유효하지 않음 (C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logoutApp(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        loginTokenService.logout(request.refreshToken(), OAuthClientType.APP);
        return ApiResponse.ok();
    }

    @Operation(
            summary = "웹 토큰 재발급",
            description = """
                    HttpOnly 쿠키의 WEB Refresh Token을 사용해 기존 토큰을 폐기하고 새 토큰을 발급합니다.

                    Access Token은 응답 본문으로, 새 Refresh Token은 HttpOnly 쿠키로 전달합니다.

                    브라우저 요청에 credentials를 포함하고 Authorization 헤더 없이 호출합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "WEB 토큰 재발급 및 회전 성공",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "회전된 refresh_token 쿠키"
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token 쿠키가 없거나 유효하지 않음 (C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "토큰을 재발급할 수 없는 계정 상태 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/token/refresh/web")
    public ApiResponse<AuthenticationResponse> refreshWeb(
            @Parameter(
                    description = "웹 로그인 시 발급된 HttpOnly Refresh Token 쿠키",
                    required = true
            )
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false)
            String refreshToken,
            HttpServletResponse response
    ) {
        AuthenticationInfo info = loginTokenService.refresh(
                refreshToken,
                OAuthClientType.WEB
        );
        return ApiResponse.success(
                authenticationResponseHandler.handle(info, response)
        );
    }

    @Operation(
            summary = "웹 로그아웃",
            description = """
                    HttpOnly 쿠키의 WEB Refresh Token을 서버에서 폐기하고 쿠키를 만료시킵니다.

                    브라우저 요청에 credentials를 포함하고 Authorization 헤더 없이 호출합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "WEB 로그아웃 및 Refresh Token 쿠키 삭제 성공",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "즉시 만료되는 refresh_token 쿠키"
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 만료되었거나 유효하지 않음 (C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/logout/web")
    public ApiResponse<Void> logoutWeb(
            @Parameter(description = "웹 로그인 시 발급된 HttpOnly Refresh Token 쿠키")
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false)
            String refreshToken,
            HttpServletResponse response
    ) {
        if (StringUtils.hasText(refreshToken)) {
            loginTokenService.logout(refreshToken, OAuthClientType.WEB);
        }
        authenticationResponseHandler.clearRefreshTokenCookie(response);
        return ApiResponse.ok();
    }
}
