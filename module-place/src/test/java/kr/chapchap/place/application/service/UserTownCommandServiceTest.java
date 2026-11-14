package kr.chapchap.place.application.service;

import kr.chapchap.place.application.command.UserTownSaveCommand;
import kr.chapchap.place.application.info.UserTownInfo;
import kr.chapchap.place.domain.entity.UserTown;
import kr.chapchap.place.domain.repository.UserTownRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTownCommandServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private UserTownRepository userTownRepository;

    private UserTownCommandService sut;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZONE);
        sut = new UserTownCommandService(userTownRepository, clock);
    }

    @Test
    void 행정동_코드가_없어도_같은_이름의_기존_동네가_있으면_새로_저장하지_않고_갱신한다() {
        // given
        UserTown existing = UserTown.create(
                1L, null, "서울특별시 동대문구 답십리동 80-2", 37.57, 127.06, LocalDateTime.now().minusDays(1));
        UserTownSaveCommand command = new UserTownSaveCommand(
                1L, null, "서울특별시 동대문구 답십리동 80-2", 37.575678501604735, 127.06165473228577);
        when(userTownRepository.findByUserIdAndAdministrativeDongCodeIsNullAndAdministrativeDongName(
                1L, "서울특별시 동대문구 답십리동 80-2"))
                .thenReturn(Optional.of(existing));
        when(userTownRepository.findByUserIdOrderBySelectedAtDesc(1L)).thenReturn(List.of(existing));

        // when
        UserTownInfo result = sut.save(command);

        // then
        verify(userTownRepository, never()).save(any());
        assertThat(result.latitude()).isEqualTo(37.575678501604735);
        assertThat(result.longitude()).isEqualTo(127.06165473228577);
    }

    @Test
    void 행정동_코드가_없고_같은_이름의_기존_동네도_없으면_새로_저장한다() {
        // given
        UserTownSaveCommand command = new UserTownSaveCommand(
                1L, null, "서울특별시 강남구 역삼동", 37.5, 127.03);
        when(userTownRepository.findByUserIdAndAdministrativeDongCodeIsNullAndAdministrativeDongName(
                1L, "서울특별시 강남구 역삼동"))
                .thenReturn(Optional.empty());
        when(userTownRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTownRepository.findByUserIdOrderBySelectedAtDesc(1L)).thenReturn(List.of());

        // when
        sut.save(command);

        // then
        verify(userTownRepository).save(any());
    }

    @Test
    void 행정동_코드가_있으면_코드_기준으로_기존_동네를_찾아_갱신한다() {
        // given
        UserTown existing = UserTown.create(
                1L, "SAMPLE00001", "서울특별시 성동구 성수1가동", 37.5, 127.05, LocalDateTime.now().minusDays(1));
        UserTownSaveCommand command = new UserTownSaveCommand(
                1L, "SAMPLE00001", "서울특별시 성동구 성수1가동", 37.5441, 127.0557);
        when(userTownRepository.findByUserIdAndAdministrativeDongCode(1L, "SAMPLE00001"))
                .thenReturn(Optional.of(existing));
        when(userTownRepository.findByUserIdOrderBySelectedAtDesc(1L)).thenReturn(List.of(existing));

        // when
        sut.save(command);

        // then
        verify(userTownRepository, never()).save(any());
        verify(userTownRepository, never())
                .findByUserIdAndAdministrativeDongCodeIsNullAndAdministrativeDongName(any(), any());
    }
}
