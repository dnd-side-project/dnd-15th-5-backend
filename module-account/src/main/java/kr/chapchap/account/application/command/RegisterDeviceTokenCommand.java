package kr.chapchap.account.application.command;

public record RegisterDeviceTokenCommand (Long userId, String fcmToken){
}
