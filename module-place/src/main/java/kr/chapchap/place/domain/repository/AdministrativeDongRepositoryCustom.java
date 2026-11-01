package kr.chapchap.place.domain.repository;

import kr.chapchap.place.domain.entity.AdministrativeDong;

import java.util.List;

public interface AdministrativeDongRepositoryCustom {

    List<AdministrativeDong> searchByExactMatch(String keyword, int limit);
}
