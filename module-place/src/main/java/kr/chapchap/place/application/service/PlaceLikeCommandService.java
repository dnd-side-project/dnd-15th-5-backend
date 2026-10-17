package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import kr.chapchap.place.domain.repository.PlaceRepository;
import kr.chapchap.place.exception.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaceLikeCommandService {

    private static final String LIKE_UNIQUE_CONSTRAINT = "uk_place_likes_user_place";
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final PlaceLikeRepository placeLikeRepository;
    private final PlaceRepository placeRepository;

    public boolean toggle(Long userId, Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        try {
            return placeLikeRepository.findByUserIdAndPlaceId(userId, placeId)
                    .map(placeLike -> {
                        placeLikeRepository.delete(placeLike);
                        // 커밋 전에 동시 해제 충돌을 확인해 이 유스케이스의 409 오류로 변환한다.
                        placeLikeRepository.flush();
                        return false;
                    })
                    .orElseGet(() -> {
                        placeLikeRepository.save(PlaceLike.builder()
                                .userId(userId)
                                .placeId(placeId)
                                .build());
                        return true;
                    });
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLike(exception)) {
                throw new BusinessException(PlaceErrorCode.LIKE_CONFLICT, exception);
            }
            throw exception;
        } catch (ObjectOptimisticLockingFailureException exception) {
            if (PlaceLike.class.getName().equals(exception.getPersistentClassName())) {
                throw new BusinessException(PlaceErrorCode.LIKE_CONFLICT, exception);
            }
            throw exception;
        }
    }

    private boolean isDuplicateLike(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && UNIQUE_VIOLATION_SQL_STATE.equals(constraintViolation.getSQLState())
                    && LIKE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
