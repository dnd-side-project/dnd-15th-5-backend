package kr.chapchap.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_terms_agreements")
public class UserTermsAgreement {

    private static final int MAX_TERMS_VERSION_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 30)
    private TermsType termsType;

    @Column(name = "terms_version", nullable = false, length = MAX_TERMS_VERSION_LENGTH)
    private String termsVersion;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserTermsAgreement create(
            Long userId,
            TermsType termsType,
            String termsVersion,
            LocalDateTime agreedAt
    ) {
        UserTermsAgreement agreement = new UserTermsAgreement();
        agreement.userId = Objects.requireNonNull(userId, "사용자 식별자는 필수입니다.");
        agreement.termsType = Objects.requireNonNull(termsType, "약관 종류는 필수입니다.");
        agreement.termsVersion = validateTermsVersion(termsVersion);
        agreement.agreedAt = Objects.requireNonNull(
                agreedAt,
                "약관 동의 시각은 필수입니다."
        );
        return agreement;
    }

    private static String validateTermsVersion(String termsVersion) {
        if (termsVersion == null || termsVersion.isBlank()) {
            throw new IllegalArgumentException("약관 버전은 비어 있을 수 없습니다.");
        }

        if (termsVersion.length() > MAX_TERMS_VERSION_LENGTH) {
            throw new IllegalArgumentException("약관 버전은 30자를 초과할 수 없습니다.");
        }

        return termsVersion;
    }
}
