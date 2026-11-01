package kr.chapchap.place.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "administrative_dongs")
public class AdministrativeDong {
    // "경북"처럼 full_name의 부분 문자열로는 찾을 수 없는 시/도 약칭.
    private static final Map<String, String> SIDO_ABBREVIATIONS = Map.of(
            "경상북도", "경북",
            "경상남도", "경남",
            "충청북도", "충북",
            "충청남도", "충남",
            "전라남도", "전남"
    );

    @Id
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "sido_name", nullable = false, length = 50)
    private String sidoName;

    @Column(name = "sigungu_name", nullable = false, length = 50)
    private String sigunguName;

    @Column(name = "dong_name", nullable = false, length = 50)
    private String dongName;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    // 검색 전용 컬럼(full_name + 시/도 약칭). 응답에는 노출하지 않고 검색 쿼리에서만 쓴다.
    @Column(name = "search_text", nullable = false, length = 160)
    private String searchText;

    @Builder
    private AdministrativeDong(String code, String sidoName, String sigunguName, String dongName,
                                String fullName, String searchText) {
        this.code = code;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.dongName = dongName;
        this.fullName = fullName;
        this.searchText = searchText;
    }

    public static AdministrativeDong create(String code, String sidoName, String sigunguName, String dongName) {
        String fullName = (sigunguName == null || sigunguName.isBlank())
                ? sidoName + " " + dongName
                : sidoName + " " + sigunguName + " " + dongName;
        String abbreviation = SIDO_ABBREVIATIONS.get(sidoName);
        String searchText = abbreviation == null ? fullName : fullName + " " + abbreviation;
        return new AdministrativeDong(code, sidoName, sigunguName, dongName, fullName, searchText);
    }
}
