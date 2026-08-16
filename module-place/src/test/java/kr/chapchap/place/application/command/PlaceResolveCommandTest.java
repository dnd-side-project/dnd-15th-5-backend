package kr.chapchap.place.application.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceResolveCommandTest {

    @Test
    void 문자열_입력의_앞뒤_공백을_제거한다() {
        // given & when
        PlaceResolveCommand command = new PlaceResolveCommand(
                " google-place-id ",
                " 테스트 가게 ",
                " 서울 강남구 테헤란로 1 ",
                37.5001,
                127.0365
        );

        // then
        assertThat(command.googlePlaceId()).isEqualTo("google-place-id");
        assertThat(command.name()).isEqualTo("테스트 가게");
        assertThat(command.roadAddress()).isEqualTo("서울 강남구 테헤란로 1");
    }

    @Test
    void Google_Place_ID가_비어_있으면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> new PlaceResolveCommand(
                " ",
                "테스트 가게",
                "서울 강남구 테헤란로 1",
                37.5001,
                127.0365
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 위경도가_유효_범위를_벗어나면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> new PlaceResolveCommand(
                "google-place-id",
                "테스트 가게",
                "서울 강남구 테헤란로 1",
                91,
                127.0365
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PlaceResolveCommand(
                "google-place-id",
                "테스트 가게",
                "서울 강남구 테헤란로 1",
                37.5001,
                Double.NaN
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
