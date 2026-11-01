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

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_towns")
public class UserTown extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(name = "administrative_dong_code", length = 20)
    private String administrativeDongCode;

    @Column(name = "administrative_dong_name", nullable = false, length = 100)
    private String administrativeDongName;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    @Builder
    private UserTown(Long userId, String administrativeDongCode, String administrativeDongName,
                      double latitude, double longitude, LocalDateTime selectedAt) {
        this.userId = userId;
        this.administrativeDongCode = administrativeDongCode;
        this.administrativeDongName = administrativeDongName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.selectedAt = selectedAt;
    }

    public static UserTown create(Long userId, String administrativeDongCode, String administrativeDongName,
                                   double latitude, double longitude, LocalDateTime selectedAt) {
        return new UserTown(userId, administrativeDongCode, administrativeDongName, latitude, longitude, selectedAt);
    }

    public void touch(String administrativeDongName, double latitude, double longitude, LocalDateTime selectedAt) {
        this.administrativeDongName = administrativeDongName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.selectedAt = selectedAt;
    }
}
