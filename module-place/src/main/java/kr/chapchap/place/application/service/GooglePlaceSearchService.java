package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;
import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;
import kr.chapchap.place.application.port.GooglePlaceTextSearchPort;
import kr.chapchap.place.application.port.PlacePhotoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class GooglePlaceSearchService {

    private static final int THUMBNAIL_MAX_WIDTH_PX = 400;

    private final GooglePlaceTextSearchPort googlePlaceTextSearchPort;
    private final PlacePhotoPort placePhotoPort;

    public Optional<GooglePlaceSearchResultInfo> search(String storeName, String address) {
        if (!StringUtils.hasText(storeName)) {
            return Optional.empty();
        }

        String textQuery = StringUtils.hasText(address)
                ? storeName.trim() + " " + address.trim()
                : storeName.trim();

        GooglePlaceTextSearchInfo candidate;
        try {
            Optional<GooglePlaceTextSearchInfo> searchResult =
                    googlePlaceTextSearchPort.searchFirst(textQuery);
            if (searchResult.isEmpty()) {
                return Optional.empty();
            }
            candidate = searchResult.get();
        } catch (BusinessException exception) {
            log.warn(
                    "Google Place 검색에 실패했습니다. code={}",
                    exception.getErrorCode().getCode()
            );
            return Optional.empty();
        }

        String thumbnailUrl = null;
        if (StringUtils.hasText(candidate.photoName())) {
            try {
                thumbnailUrl = placePhotoPort.resolvePhotoUri(
                        candidate.photoName(),
                        THUMBNAIL_MAX_WIDTH_PX
                ).toString();
            } catch (BusinessException exception) {
                log.warn(
                        "Google Place 검색 결과의 썸네일 조회에 실패했습니다. googlePlaceId={}, code={}",
                        candidate.googlePlaceId(),
                        exception.getErrorCode().getCode()
                );
            }
        }

        return Optional.of(new GooglePlaceSearchResultInfo(
                candidate.googlePlaceId(),
                candidate.placeName(),
                candidate.roadAddress(),
                candidate.latitude(),
                candidate.longitude(),
                thumbnailUrl
        ));
    }
}
