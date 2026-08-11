package kr.chapchap.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.chapchap.core.persistence.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 10;
    private static final int MAX_PROFILE_IMAGE_KEY_LENGTH = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false, length = MAX_NICKNAME_LENGTH)
    private String nickname;

    @Column(name = "profile_image_key", length = 1024)
    private String profileImageKey;

    // TODO: 이메일 수집 미확정, 현재 소셜 제공자의 이메일 동의항목은 사용하지 않음
    @Column(name = "email", length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public static User create(String nickname) {
        User user = new User();
        user.nickname = validateNickname(nickname);
        user.status = UserStatus.PENDING_TERMS;
        return user;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isPendingTerms() {
        return status == UserStatus.PENDING_TERMS;
    }

    public void completeTermsAgreement() {
        if (!isPendingTerms()) {
            throw new IllegalStateException("약관 동의 대기 상태에서만 가입을 완료할 수 있습니다.");
        }

        this.status = UserStatus.ACTIVE;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        LocalDateTime requiredWithdrawnAt = Objects.requireNonNull(
                withdrawnAt,
                "탈퇴 시각은 필수입니다."
        );
        if (!isActive()) {
            throw new IllegalStateException("활성 상태에서만 탈퇴할 수 있습니다.");
        }

        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = requiredWithdrawnAt;
    }

    public void updateNickname(String nickname) {
        this.nickname = validateNickname(nickname);
    }

    public void updateProfileImageKey(String profileImageKey) {
        if (profileImageKey == null || profileImageKey.isBlank()) {
            throw new IllegalArgumentException("프로필 이미지 Object Key는 비어 있을 수 없습니다.");
        }
        if (profileImageKey.length() > MAX_PROFILE_IMAGE_KEY_LENGTH) {
            throw new IllegalArgumentException("프로필 이미지 Object Key는 1024자를 초과할 수 없습니다.");
        }

        this.profileImageKey = profileImageKey;
    }

    public void deleteProfileImage() {
        this.profileImageKey = null;
    }

    private static String validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }

        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() < MIN_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("닉네임은 2자 이상이어야 합니다.");
        }
        if (trimmedNickname.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("닉네임은 10자를 초과할 수 없습니다.");
        }

        return trimmedNickname;
    }
}
