package kr.chapchap.account.domain.entity;

import org.junit.jupiter.api.Test;

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 비어 있을 수 없습니다.");
    }

    @Test
    void 닉네임이_10자를_초과하면_사용자를_생성할_수_없다() {
        // given
        String nickname = "찹".repeat(11);

        // when & then
        assertThatThrownBy(() -> User.create(nickname))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 10자를 초과할 수 없습니다.");
    }
}
