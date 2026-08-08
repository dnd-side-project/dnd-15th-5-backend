package kr.chapchap.place.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.chapchap.core.persistence.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {

    // location(GEOGRAPHY)는 위치 기반 조회를 만들 때 다루기로 하고 현재 엔티티 필드에서는 제외함

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "road_address", nullable = false)
    private String roadAddress;

    @Column(name = "administrative_dong_code", nullable = false, length = 20)
    private String administrativeDongCode;

    @Column(name = "administrative_dong_name", nullable = false, length = 100)
    private String administrativeDongName;

    @Builder
    private Place(String googlePlaceId, String name, String roadAddress, String administrativeDongCode,
                   String administrativeDongName) {
        this.googlePlaceId = googlePlaceId;
        this.name = name;
        this.roadAddress = roadAddress;
        this.administrativeDongCode = administrativeDongCode;
        this.administrativeDongName = administrativeDongName;
    }
}
