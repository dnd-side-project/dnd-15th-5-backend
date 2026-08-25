package kr.chapchap.account.api.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenRegisterRequest(@NotBlank String fcmToken) {
}
