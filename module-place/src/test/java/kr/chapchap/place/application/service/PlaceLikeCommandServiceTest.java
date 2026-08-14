package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import kr.chapchap.place.domain.repository.PlaceRepository;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceLikeCommandServiceTest {

    @Mock
    private PlaceLikeRepository placeLikeRepository;

    @Mock
    private PlaceRepository placeRepository;

    private PlaceLikeCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceLikeCommandService(placeLikeRepository, placeRepository);
    }

    @Test
    void 좋아요_안한_상태에서_토글하면_좋아요가_등록되고_true를_반환한다() {
        // given
        when(placeRepository.existsById(101L)).thenReturn(true);
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
        when(placeRepository.existsById(101L)).thenReturn(true);
        PlaceLike existing = PlaceLike.builder().userId(1L).placeId(101L).build();
        when(placeLikeRepository.findByUserIdAndPlaceId(1L, 101L)).thenReturn(Optional.of(existing));

        // when
        boolean liked = sut.toggle(1L, 101L);

        // then
        assertThat(liked).isFalse();
        verify(placeLikeRepository, times(1)).delete(existing);
        verify(placeLikeRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_장소면_예외를_던지고_좋아요_레포지토리는_건드리지_않는다() {
        // given
        when(placeRepository.existsById(999L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.toggle(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);
        verifyNoInteractions(placeLikeRepository);
    }
}
