package kr.chapchap.place.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kr.chapchap.place.domain.entity.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresPlaceCreateAdapterTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query insertQuery;

    @Mock
    private Query findQuery;

    private PostgresPlaceCreateAdapter sut;
    private Place place;

    @BeforeEach
    void setUp() {
        sut = new PostgresPlaceCreateAdapter(entityManager);
        place = Place.create(
                "google-place-id",
                "테스트 가게",
                "서울 강남구 테헤란로 1",
                "11680640",
                "역삼1동",
                37.5001,
                127.0365
        );
    }

    @Test
    void Google_Place_ID가_없으면_장소를_삽입하고_생성된_ID를_반환한다() {
        // given
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(insertQuery);
        when(insertQuery.getResultList()).thenReturn(List.of(101L));

        // when
        Long result = sut.createOrGet(place);

        // then
        assertThat(result).isEqualTo(101L);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("ON CONFLICT (google_place_id) DO NOTHING")
                .contains("ST_MakePoint(:longitude, :latitude)");
        verify(insertQuery).setParameter("longitude", 127.0365);
        verify(insertQuery).setParameter("latitude", 37.5001);
    }

    @Test
    void 동시_삽입으로_충돌하면_기존_장소를_덮어쓰지_않고_ID를_조회해_반환한다() {
        // given
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(insertQuery, findQuery);
        when(insertQuery.getResultList()).thenReturn(List.of());
        when(findQuery.getResultList()).thenReturn(List.of(202L));

        // when
        Long result = sut.createOrGet(place);

        // then
        assertThat(result).isEqualTo(202L);
        verify(entityManager, times(2)).createNativeQuery(
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(findQuery).setParameter("googlePlaceId", "google-place-id");
    }
}
