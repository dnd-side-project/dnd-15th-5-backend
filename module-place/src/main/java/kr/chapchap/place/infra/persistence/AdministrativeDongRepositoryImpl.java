package kr.chapchap.place.infra.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.chapchap.place.domain.entity.AdministrativeDong;
import kr.chapchap.place.domain.entity.QAdministrativeDong;
import kr.chapchap.place.domain.repository.AdministrativeDongRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class AdministrativeDongRepositoryImpl implements AdministrativeDongRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdministrativeDong> searchByExactMatch(String keyword, int limit) {
        QAdministrativeDong administrativeDong = QAdministrativeDong.administrativeDong;

        List<String> tokens = Arrays.stream(keyword.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
        String firstToken = tokens.getFirst();

        BooleanBuilder condition = new BooleanBuilder();
        for (String token : tokens) {
            condition.and(administrativeDong.searchText.contains(token));
        }

        NumberExpression<Integer> matchRank = new CaseBuilder()
                .when(administrativeDong.dongName.eq(keyword)).then(0)
                .when(administrativeDong.dongName.startsWith(firstToken)).then(1)
                .otherwise(2);

        return queryFactory
                .selectFrom(administrativeDong)
                .where(condition)
                .orderBy(matchRank.asc(), administrativeDong.fullName.asc())
                .limit(limit)
                .fetch();
    }
}
