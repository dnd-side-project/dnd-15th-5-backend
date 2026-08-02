package kr.chapchap.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.account.api.response.AccountTestResponse;
import kr.chapchap.core.web.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "계정 API")
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final String ACCOUNT_MODULE = "account";
    private static final String AVAILABLE_STATUS = "UP";

    @Operation(
            summary = "계정 API 연결 확인",
            description = "Swagger 문서와 클라이언트 연동을 확인하기 위한 임시 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "연결 확인 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 테스트 파라미터 누락",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {"code": "C003", "message": "필수 요청 파라미터가 누락되었습니다.", "data": {"testParam": "필수 요청 파라미터입니다."}}"""
                            )
                    )
            )
    })
    @GetMapping("/test")
    public ApiResponse<AccountTestResponse> checkAccountApi(
            @RequestParam("testParam") String testParam
    ) {
        AccountTestResponse response = new AccountTestResponse(ACCOUNT_MODULE, AVAILABLE_STATUS);
        return ApiResponse.success(response);
    }
}
