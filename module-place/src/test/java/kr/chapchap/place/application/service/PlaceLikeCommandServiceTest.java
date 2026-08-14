package kr.chapchap.place.application.service;

import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceLikeCommandServiceTest {

    @Mock
    private PlaceLikeRepository placeLikeRepository;

    private PlaceLikeCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceLikeCommandService(placeLikeRepository);
    }

    @Test
    void 좋아요_안한_상태에서_토글하면_좋아요가_등록되고_true를_반환한다() {
        // given
        when(placeLikeRepository.findByUserIdAndPlaceId(1L, 101L)).thenReturn(Optional.empty());

        // when
        boolean liked = sut.toggle(1L, 101L);

        // then
        assertThat(liked).isTrue();
        verify(placeLikeRepository, times(1)).save(any());
        verify(placeLikeRepository, never()).delete(any());
    }

    @Test
    void 이미_좋아요한_상태에서_토글하면_좋아요가_취소되고_false를_반환한다() {
        // given
        PlaceLike existing = PlaceLike.builder().userId(1L).placeId(101L).build();
        when(placeLikeRepository.findByUserIdAndPlaceId(1L, 101L)).thenReturn(Optional.of(existing));

        // when
        boolean liked = sut.toggle(1L, 101L);

        // then
        assertThat(liked).isFalse();
        verify(placeLikeRepository, times(1)).delete(existing);
        verify(placeLikeRepository, never()).save(any());
    }
}
