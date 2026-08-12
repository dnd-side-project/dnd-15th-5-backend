package kr.chapchap.account.domain.entity;

import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void 신규_사용자는_약관_동의_대기_상태로_생성된다() {
        // when
        User user = User.create("찹찹이");

        // then
        assertThat(user.isPendingTerms()).isTrue();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void 약관_동의를_완료하면_사용자가_활성_상태가_된다() {
        // given
        User user = User.create("찹찹이");

        // when
        user.completeTermsAgreement();

        // then
        assertThat(user.isPendingTerms()).isFalse();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void 약관_동의_대기_상태가_아니면_가입을_완료할_수_없다() {
        // given
        User user = User.create("찹찹이");
        user.completeTermsAgreement();

        // when & then
        assertThatThrownBy(user::completeTermsAgreement)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.TERMS_AGREEMENT_NOT_ALLOWED)
                );
    }

    @Test
    void 활성_사용자가_탈퇴하면_상태와_탈퇴_시각을_기록한다() {
        // given
        User user = User.create("찹찹이");
        user.completeTermsAgreement();
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 11, 12, 0);

        // when
        user.withdraw(withdrawnAt);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getWithdrawnAt()).isEqualTo(withdrawnAt);
    }

    @Test
    void 활성_상태가_아닌_사용자는_탈퇴할_수_없다() {
        // given
        User user = User.create("찹찹이");

        // when & then
        assertThatThrownBy(() -> user.withdraw(LocalDateTime.now()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.ACCOUNT_WITHDRAWAL_NOT_ALLOWED)
                );
    }

    @Test
    void 닉네임의_앞뒤_공백을_제거한다() {
        // when
        User user = User.create("  찹찹이  ");

        // then
        assertThat(user.getNickname()).isEqualTo("찹찹이");
    }

    @Test
    void 닉네임이_비어_있으면_사용자를_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> User.create(" "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.NICKNAME_REQUIRED)
                );
    }

    @Test
    void 닉네임이_2자_미만이거나_10자를_초과하면_사용자를_생성할_수_없다() {
        // given
        String nickname = "찹".repeat(11);

        // when & then
        assertThatThrownBy(() -> User.create("찹"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.NICKNAME_TOO_SHORT)
                );
        assertThatThrownBy(() -> User.create(nickname))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.NICKNAME_TOO_LONG)
                );
    }

    @Test
    void 닉네임을_수정하면_앞뒤_공백을_제거한다() {
        // given
        User user = User.create("기존닉네임");

        // when
        user.updateNickname("  새닉네임  ");

        // then
        assertThat(user.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void 닉네임은_빈_닉네임으로_수정할_수_없다() {
        // given
        User user = User.create("기존닉네임");

        // when & then
        assertThatThrownBy(() -> user.updateNickname(" "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.NICKNAME_REQUIRED)
                );
    }

    @Test
    void 프로필_이미지_Key를_설정하고_삭제한다() {
        // given
        User user = User.create("찹찹이");

        // when
        user.updateProfileImageKey("profiles/1/profile-image");

        // then
        assertThat(user.getProfileImageKey()).isEqualTo("profiles/1/profile-image");

        // when
        user.deleteProfileImage();

        // then
        assertThat(user.getProfileImageKey()).isNull();
    }
}
