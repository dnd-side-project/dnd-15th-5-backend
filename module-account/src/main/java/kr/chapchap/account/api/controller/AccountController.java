package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.chapchap.account.api.request.AccountUpdateRequest;
import kr.chapchap.account.api.request.DeviceTokenRegisterRequest;
import kr.chapchap.account.api.response.AccountResponse;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.command.RegisterDeviceTokenCommand;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.AccountCommandService;
import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.account.application.service.AccountWithdrawalService;
import kr.chapchap.account.application.service.DeviceTokenCommandService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account", description = "사용자 계정 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountQueryService accountQueryService;
    private final AccountCommandService accountCommandService;
    private final AccountWithdrawalService accountWithdrawalService;
    private final AuthenticationResponseHandler authenticationResponseHandler;
    private final DeviceTokenCommandService deviceTokenCommandService;

    @Operation(
            summary = "내 정보 조회",
            description = "Access Token으로 인증된 사용자의 기본 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근할 수 없는 사용자 상태 또는 권한 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/me")
    public ApiResponse<AccountResponse> getMyAccount(
            @ChapChapUserId Long userId
    ) {
        AccountInfo info = accountQueryService.getAccount(userId);
        return ApiResponse.success(AccountResponse.from(info));
    }

    @Operation(
            summary = "내 정보 수정",
            description = """
                    닉네임과 프로필 이미지를 선택적으로 수정합니다.

                    - `profileImage` 전달: 프로필 이미지를 등록하거나 교체합니다.
                    - `deleteProfileImage=true`: 기존 프로필 이미지를 삭제합니다.
                    - 이미지 관련 값을 전달하지 않으면 기존 프로필 이미지를 유지합니다.
                    - 이미지 파일과 삭제 요청은 동시에 사용할 수 없습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 정보 수정 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 또는 계정 수정 값이 유효하지 않음 (C001, A004~A006, A008, A009)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "413",
                    description = "프로필 이미지 또는 서버 업로드 크기 제한 초과 (A007, C008)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근할 수 없는 사용자 상태 또는 권한 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "프로필 이미지 저장소 연동 실패 (C007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AccountResponse> updateMyAccount(
            @ChapChapUserId Long userId,
            @Valid @ModelAttribute AccountUpdateRequest request
    ) {
        AccountInfo info = accountCommandService.updateAccount(request.toCommand(userId));
        return ApiResponse.success(AccountResponse.from(info));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    Kakao는 연결 해제와 회원 탈퇴를 즉시 처리하고 200 OK를 반환합니다.

                    Google은 202 Accepted와 Location 헤더를 반환하며, 클라이언트가 해당 URI로 이동해 재인증을 완료하면 callback에서 회원 탈퇴를 처리합니다.

                    Google의 202 Accepted 응답은 회원 탈퇴 완료를 의미하지 않습니다.

                    회원 탈퇴 완료 후 WEB과 APP은 보관 중인 Access Token을 삭제해야 합니다.

                    WEB은 Refresh Token 쿠키가 만료되며, APP은 로컬에 저장된 Refresh Token도 함께 삭제해야 합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Kakao 회원 탈퇴 완료",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "탈퇴할 수 없는 사용자 상태 또는 권한 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "카카오 연결 해제 실패 (C007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Google 회원 탈퇴 절차 시작. 탈퇴 완료 전이며 Location 헤더의 URI로 이동 필요",
                    useReturnTypeSchema = true,
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Location",
                            description = "Google 재인증 URI"
                    )
            )
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMyAccount(
            @ChapChapUserId Long userId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response
    ) {
        Optional<URI> authorizationUri = accountWithdrawalService.startWithdrawal(
                userId,
                OAuthClientType.fromClaim(jwt.getClaimAsString("client_type"))
        );
        if (authorizationUri.isPresent()) {
            return ResponseEntity.accepted()
                    .location(authorizationUri.get())
                    .body(ApiResponse.ok());
        }

        authenticationResponseHandler.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/me/device-token")
    public ApiResponse<Void> registerDeviceToken(
            @ChapChapUserId Long userId,
            @Valid @RequestBody DeviceTokenRegisterRequest request
            ){
        deviceTokenCommandService.registerToken(new RegisterDeviceTokenCommand(userId, request.fcmToken()));
        return ApiResponse.ok();
    }

    @DeleteMapping("/me/device-token")
    public ApiResponse<Void> unregisterDeviceToken(
            @ChapChapUserId Long userId
    ) {
        deviceTokenCommandService.unregisterToken(userId);
        return ApiResponse.ok();
    }
}
