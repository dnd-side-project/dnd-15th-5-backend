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

import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "social_accounts")
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    public static SocialAccount create(
            Long userId,
            SocialProvider provider,
            String providerUserId
    ) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.userId = Objects.requireNonNull(userId, "사용자 식별자는 필수입니다.");
        socialAccount.provider = Objects.requireNonNull(
                provider,
                "소셜 로그인 제공자는 필수입니다."
        );
        socialAccount.providerUserId = validateProviderUserId(providerUserId);
        return socialAccount;
    }

    private static String validateProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 로그인 사용자 식별값은 비어 있을 수 없습니다.");
        }

        return providerUserId;
    }
}
