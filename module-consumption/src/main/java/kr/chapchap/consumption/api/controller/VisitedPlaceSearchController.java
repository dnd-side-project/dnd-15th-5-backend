package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.consumption.api.response.VisitedPlaceSearchResponse;
import kr.chapchap.consumption.application.command.VisitedPlaceSearchCommand;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.service.VisitedPlaceSearchService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Place", description = "장소 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/places")
public class VisitedPlaceSearchController {

    private final VisitedPlaceSearchService visitedPlaceSearchService;

    @Operation(
            summary = "방문 장소 검색",
            description = """
                    인증된 사용자의 소비 기록에 존재하는 장소를 이름 또는 도로명주소로 검색합니다.

                    동일한 장소의 소비 기록이 여러 개라면 가장 최근 방문을 기준으로 정렬합니다.

                    검색 결과는 최대 5개까지 반환하며, 다음 페이지 조회 시 동일한 검색어와 함께 이전 응답의 nextCursor를 전달합니다.

                    장소 사진이 없거나 Google 사진 조회에 실패하면 thumbnailUrl과 googleMapsUri는 null로 반환됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "방문 장소 검색 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값이 유효하지 않음 (C001, CONSUMPTION014~016)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않음 (C004, C006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한이 없음 (C005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/visited/search")
    public ApiResponse<VisitedPlaceSearchResponse> searchVisitedPlaces(
            @ChapChapUserId Long userId,
            @Parameter(
                    description = "장소명 또는 도로명주소에 포함될 검색어 (1~100자)",
                    example = "신논현"
            )
            @RequestParam String keyword,
            @Parameter(
                    description = "이전 응답의 nextCursor, 첫 조회 또는 검색어 변경 시 생략"
            )
            @RequestParam(required = false) String cursor,
            @Parameter(
                    description = "페이지 크기 (1~5, 기본값 5)",
                    example = "5"
            )
            @RequestParam(defaultValue = "5") int size
    ) {
        VisitedPlaceSearchCommand command = new VisitedPlaceSearchCommand(userId, keyword, cursor, size);
        VisitedPlaceSearchInfo info = visitedPlaceSearchService.search(command);
        return ApiResponse.success(VisitedPlaceSearchResponse.from(info));
    }
}
