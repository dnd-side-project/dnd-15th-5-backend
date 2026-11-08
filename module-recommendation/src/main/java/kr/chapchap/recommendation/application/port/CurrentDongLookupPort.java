package kr.chapchap.recommendation.application.port;

public interface CurrentDongLookupPort {
    String findFullName(double latitude, double longitude);
}
