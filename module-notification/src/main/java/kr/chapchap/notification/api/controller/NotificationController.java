package kr.chapchap.notification.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.notification.api.response.NotificationResponse;
import kr.chapchap.notification.application.service.NotificationCommandService;
import kr.chapchap.notification.application.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    @Operation(
            summary = "알림 목록 조회",
            description = "호출 시 모든 알림 조회처리"
    )
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @ChapChapUserId Long userId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<NotificationResponse> responses = notificationQueryService.getNotifications(userId, cursorId, size).stream()
                .map(NotificationResponse::from)
                .toList();

        notificationCommandService.markAllAsRead(userId);

        return ApiResponse.success(responses);
    }
    @Operation(
            summary = "읽지 않은 알림 확인 조회",
            description = "홈화면에서 알림 존재 유무를 확인합니다."
    )
    @GetMapping("/unread-exists")
    public ApiResponse<Boolean> hasUnread(@ChapChapUserId Long userId) {
        return ApiResponse.success(notificationQueryService.hasUnread(userId));
    }
}
