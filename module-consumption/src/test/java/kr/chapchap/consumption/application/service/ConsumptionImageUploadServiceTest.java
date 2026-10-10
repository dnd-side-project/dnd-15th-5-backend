package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionImageUploadCommand;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumptionImageUploadServiceTest {
    @Mock private ConsumptionImageStorage storage;
    @Mock private ConsumptionImageCommandService commands;

    @Test
    void DB_저장이_실패하면_업로드한_파일을_삭제한다() throws Exception {
        // given
        byte[] content = png();
        given(storage.store(1L, content, "image/png")).willReturn("key");
        RuntimeException failure = new IllegalStateException("DB 저장 실패");
        given(commands.saveTemporary(eq(1L), eq("key"), eq("image/png"), eq((long) content.length), any()))
                .willThrow(failure);
        // when & then
        assertThatThrownBy(() -> service().upload(new ConsumptionImageUploadCommand(1L, content))).isSameAs(failure);
        then(storage).should().delete("key");
    }

    @Test
    void 보상_삭제도_실패하면_원래_예외에_삭제_실패를_남긴다() throws Exception {
        // given
        byte[] content = png();
        given(storage.store(1L, content, "image/png")).willReturn("key");
        RuntimeException failure = new IllegalStateException("DB 저장 실패");
        RuntimeException deleteFailure = new IllegalStateException("S3 삭제 실패");
        given(commands.saveTemporary(anyLong(), anyString(), anyString(), anyLong(), any())).willThrow(failure);
        willThrow(deleteFailure).given(storage).delete("key");
        // when & then
        assertThatThrownBy(() -> service().upload(new ConsumptionImageUploadCommand(1L, content)))
                .isSameAs(failure).satisfies(exception -> assertThat(exception.getSuppressed()).containsExactly(deleteFailure));
    }

    @Test
    void 이미지가_유효하지_않으면_S3와_DB를_호출하지_않는다() {
        // given
        byte[] content = {1, 2, 3};
        // when & then
        assertThatThrownBy(() -> service().upload(new ConsumptionImageUploadCommand(1L, content)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ConsumptionErrorCode.INVALID_IMAGE));
        then(storage).shouldHaveNoInteractions();
        then(commands).shouldHaveNoInteractions();
    }

    private ConsumptionImageUploadService service() {
        return new ConsumptionImageUploadService(new ImageValidator(), storage, commands, Clock.systemUTC());
    }

    private byte[] png() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }
}
