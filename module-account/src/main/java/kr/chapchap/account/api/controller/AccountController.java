package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.chapchap.account.api.request.AccountUpdateRequest;
import kr.chapchap.account.api.response.AccountResponse;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.service.AccountCommandService;
import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account", description = "사용자 계정 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountQueryService accountQueryService;
    private final AccountCommandService accountCommandService;

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
                    description = "수정할 값이 없거나 요청 값이 유효하지 않음 (C001)",
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
}
