package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.OAuthFlowService;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Social OAuth", description = "소셜 OAuth 화면 이동 및 콜백 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/oauth/{provider}")
public class SocialOAuthController {

    private final OAuthFlowService oauthFlowService;

    @Operation(
            summary = "소셜 로그인 시작",
            description = """
                    소셜 로그인 제공자, 클라이언트 유형과 PKCE codeChallenge를 저장한 뒤

                    브라우저를 해당 제공자의 인증 화면으로 이동시킵니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "소셜 로그인 제공자의 인증 화면으로 이동",
                    headers = @Header(name = "Location", description = "OAuth 인증 URL")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "제공자·client·codeChallenge 형식 오류 (C001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/start")
    public ResponseEntity<Void> start(
            @Parameter(
                    description = "소셜 로그인 제공자",
                    example = "google",
                    schema = @Schema(allowableValues = {"kakao", "google"})
            )
            @PathVariable String provider,
            @Parameter(
                    description = "로그인을 시작한 클라이언트 유형",
                    example = "WEB",
                    schema = @Schema(allowableValues = {"WEB", "APP"})
            )
            @RequestParam OAuthClientType client,
            @Parameter(
                    description = "PKCE S256 방식으로 생성한 43자 codeChallenge",
                    example = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
            )
            @RequestParam String codeChallenge
    ) {
        URI authorizationUri = oauthFlowService.createAuthorizationUri(
                provider,
                client,
                codeChallenge
        );
        return ResponseEntity.status(HttpStatus.FOUND).location(authorizationUri).build();
    }

    @Operation(
            summary = "소셜 로그인 콜백",
            description = """
                    소셜 로그인 제공자가 호출하는 콜백입니다. 인증 결과를 처리한 뒤 WEB 또는 APP 리디렉션 URI로

                    일회용 loginCode를 전달합니다. 취소하거나 처리에 실패하면 error를 전달합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "클라이언트 리디렉션 URI로 이동",
                    headers = @Header(
                            name = "Location",
                            description = "loginCode 또는 error가 포함된 WEB/APP 리디렉션 URI"
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 state 파라미터 누락 (C003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "OAuth state가 만료되었거나 유효하지 않음 (C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @Parameter(description = "소셜 로그인 제공자")
            @PathVariable String provider,
            @Parameter(description = "소셜 로그인 제공자가 발급한 Authorization Code")
            @RequestParam(required = false) String code,
            @Parameter(description = "로그인 시작 시 서버가 발급한 일회용 OAuth state")
            @RequestParam String state
    ) {
        URI clientRedirectUri = StringUtils.hasText(code)
                ? oauthFlowService.handleCallback(provider, code, state)
                : oauthFlowService.handleCancelledCallback(provider, state);
        return ResponseEntity.status(HttpStatus.FOUND).location(clientRedirectUri).build();
    }

}
