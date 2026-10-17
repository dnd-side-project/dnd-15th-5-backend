package kr.chapchap.place.application.service;

import kr.chapchap.place.application.command.LikedPlaceSearchCommand;
import kr.chapchap.place.application.info.LikedPlaceScrollInfo;
import kr.chapchap.place.application.info.LikedPlaceScrollInfo.LikedPlaceInfo;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.info.PlaceSummaryInfo;
import kr.chapchap.place.application.port.PlaceCategoryLookupPort;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class PlaceLikeQueryService {

    private final PlaceLikeRepository placeLikeRepository;
    private final PlaceCategoryLookupPort placeCategoryLookupPort;
    private final PlaceQueryService placeQueryService;
    private final PlacePhotoService placePhotoService;

    public LikedPlaceScrollInfo findLikedPlaces(LikedPlaceSearchCommand command) {
        // shortcut: 초기에는 사용자의 좋아요 전체를 집계한다. 목록 규모로 조회 지연이 생기면 DB 필터·페이징으로 전환한다.
        List<PlaceLike> likes = placeLikeRepository.findByUserIdOrderByIdDesc(command.userId());
        if (likes.isEmpty()) {
            return new LikedPlaceScrollInfo(List.of(), List.of(), false, null);
        }
        Map<Long, String> categoriesByPlaceId = placeCategoryLookupPort.findCategoriesByPlaceIds(
                likes.stream().map(PlaceLike::getPlaceId).toList()
        );
        List<String> categories = likes.stream()
                .map(like -> categoriesByPlaceId.get(like.getPlaceId()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<PlaceLike> candidates = likes.stream()
                .filter(like -> command.cursorId() == null || like.getId() < command.cursorId())
                .filter(like -> command.category() == null
                        || command.category().equals(categoriesByPlaceId.get(like.getPlaceId())))
                .limit(command.size() + 1L)
                .toList();
        boolean hasNext = candidates.size() > command.size();
        List<PlaceLike> page = candidates.stream().limit(command.size()).toList();
        if (page.isEmpty()) {
            return new LikedPlaceScrollInfo(List.of(), categories, false, null);
        }

        Map<Long, PlaceSummaryInfo> summaries = placeQueryService.findSummariesByIds(
                page.stream().map(PlaceLike::getPlaceId).toList()
        );
        Map<Long, String> googlePlaceIds = new LinkedHashMap<>();
        page.forEach(like -> googlePlaceIds.put(
                like.getPlaceId(), summaries.get(like.getPlaceId()).googlePlaceId()
        ));
        // 외부 사진 요청 중 DB 트랜잭션을 유지하지 않는다.
        Map<Long, List<PlacePhotoInfo>> photos = placePhotoService.findPhotos(googlePlaceIds);
        List<LikedPlaceInfo> places = page.stream().map(like -> {
            Long placeId = like.getPlaceId();
            PlaceSummaryInfo summary = summaries.get(placeId);
            return new LikedPlaceInfo(
                    placeId, summary.name(), categoriesByPlaceId.get(placeId), summary.address(),
                    summary.latitude(), summary.longitude(), photos.getOrDefault(placeId, List.of())
            );
        }).toList();
        return new LikedPlaceScrollInfo(
                places, categories, hasNext, hasNext ? page.getLast().getId() : null
        );
    }
}
