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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

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

    @Column(name = "location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @Builder
    private Place(String googlePlaceId, String name, String roadAddress, String administrativeDongCode,
                   String administrativeDongName, Point location) {
        this.googlePlaceId = googlePlaceId;
        this.name = name;
        this.roadAddress = roadAddress;
        this.administrativeDongCode = administrativeDongCode;
        this.administrativeDongName = administrativeDongName;
        this.location = location;
    }

    public static Place create(
            String googlePlaceId,
            String name,
            String roadAddress,
            String administrativeDongCode,
            String administrativeDongName,
            double latitude,
            double longitude
    ) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        return new Place(
                googlePlaceId,
                name,
                roadAddress,
                administrativeDongCode,
                administrativeDongName,
                location
        );
    }

    public double getLatitude() {
        return location.getY();
    }

    public double getLongitude() {
        return location.getX();
    }
}
