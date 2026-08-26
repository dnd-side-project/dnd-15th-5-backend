package kr.chapchap.account.exception;

import kr.chapchap.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {

    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "A001", "닉네임은 비어 있을 수 없습니다."),
    NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "A002", "닉네임은 2자 이상이어야 합니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "A003", "닉네임은 10자를 초과할 수 없습니다."),
    ACCOUNT_UPDATE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "A004", "수정할 값을 하나 이상 입력해야 합니다."),
    PROFILE_IMAGE_UPDATE_CONFLICT(HttpStatus.BAD_REQUEST, "A005", "프로필 이미지 변경과 삭제를 동시에 요청할 수 없습니다."),
    INVALID_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "A006", "올바른 프로필 이미지 파일이 아닙니다."),
    PROFILE_IMAGE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "A007", "프로필 이미지는 5MB 이하여야 합니다."),
    UNSUPPORTED_PROFILE_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "A008", "프로필 이미지는 JPEG 또는 PNG 형식만 지원합니다."),
    PROFILE_IMAGE_DIMENSION_EXCEEDED(HttpStatus.BAD_REQUEST, "A009", "프로필 이미지 해상도는 4096x4096 이하여야 합니다."),
    ACCOUNT_WITHDRAWAL_NOT_ALLOWED(HttpStatus.CONFLICT, "A010", "활성 상태에서만 탈퇴할 수 있습니다."),
    TERMS_AGREEMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "A011", "약관 동의 대기 상태에서만 가입을 완료할 수 있습니다."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "A012", "탈퇴한 계정은 로그인할 수 없습니다."),
    INVALID_FCM_TOKEN(HttpStatus.BAD_REQUEST, "A013", "FCM 토큰이 유효하지 않습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST,"A014","해당 계정이 유효하지 않습니다")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
