package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.KakaoOAuthFlowService;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;

@Tag(name = "Kakao OAuth", description = "카카오 OAuth 화면 이동 및 콜백 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/oauth/kakao")
public class KakaoOAuthController {

    private final KakaoOAuthFlowService kakaoOAuthFlowService;

    @Operation(
            summary = "카카오 로그인 시작",
            description = "클라이언트 유형과 PKCE codeChallenge를 저장한 뒤\n\n"
                    + "브라우저를 카카오 인증 화면으로 이동시킵니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "카카오 인증 화면으로 이동",
                    headers = @Header(name = "Location", description = "카카오 인증 URL")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 파라미터 누락 또는 client/codeChallenge 형식 오류 (C001, C003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/start")
    public ResponseEntity<Void> start(
            @Parameter(
                    description = "로그인을 시작한 클라이언트 유형",
                    example = "WEB",
                    schema = @Schema(allowableValues = {"WEB", "APP"})
            )
            @RequestParam String client,
            @Parameter(
                    description = "PKCE S256 방식으로 생성한 43자 codeChallenge",
                    example = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
            )
            @RequestParam String codeChallenge
    ) {
        URI authorizationUri = kakaoOAuthFlowService.createAuthorizationUri(
                parseClientType(client),
                codeChallenge
        );
        return ResponseEntity.status(HttpStatus.FOUND).location(authorizationUri).build();
    }

    @Operation(
            summary = "카카오 로그인 콜백",
            description = "카카오가 호출하는 콜백입니다. 인증 결과를 처리한 뒤 WEB 또는 APP 리디렉션 URI로\n\n"
                    + "일회용 loginCode를 전달합니다. 취소하거나 처리에 실패하면 error를 전달합니다."
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
            @Parameter(description = "카카오가 발급한 Authorization Code")
            @RequestParam(required = false) String code,
            @Parameter(description = "로그인 시작 시 서버가 발급한 일회용 OAuth state")
            @RequestParam String state
    ) {
        URI clientRedirectUri = StringUtils.hasText(code)
                ? kakaoOAuthFlowService.handleCallback(code, state)
                : kakaoOAuthFlowService.handleCancelledCallback(state);
        return ResponseEntity.status(HttpStatus.FOUND).location(clientRedirectUri).build();
    }

    private OAuthClientType parseClientType(String client) {
        try {
            return OAuthClientType.valueOf(client.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
