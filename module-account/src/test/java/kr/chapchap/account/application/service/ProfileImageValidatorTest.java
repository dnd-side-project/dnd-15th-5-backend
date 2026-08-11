package kr.chapchap.account.application.service;

import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageValidatorTest {

    private static final int MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    private final ProfileImageValidator profileImageValidator = new ProfileImageValidator();

    @Test
    void PNG_이미지를_검증하면_실제_Content_Type을_반환한다() {
        // when
        String contentType = profileImageValidator.validateAndGetContentType(PNG_IMAGE);

        // then
        assertThat(contentType).isEqualTo("image/png");
    }

    @Test
    void JPEG_이미지를_검증하면_실제_Content_Type을_반환한다() throws IOException {
        // given
        byte[] jpegImage = createImage("jpeg", 1, 1);

        // when
        String contentType = profileImageValidator.validateAndGetContentType(jpegImage);

        // then
        assertThat(contentType).isEqualTo("image/jpeg");
    }

    @Test
    void 이미지가_비어_있거나_허용_크기를_초과하면_검증할_수_없다() {
        // given
        byte[] oversizedImage = new byte[MAX_PROFILE_IMAGE_SIZE + 1];

        // when & then
        assertImageError(new byte[0], AccountErrorCode.INVALID_PROFILE_IMAGE);
        assertImageError(oversizedImage, AccountErrorCode.PROFILE_IMAGE_SIZE_EXCEEDED);
    }

    @Test
    void 손상된_이미지는_검증할_수_없다() {
        // given
        byte[] corruptedImage = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        // when & then
        assertImageError(corruptedImage, AccountErrorCode.INVALID_PROFILE_IMAGE);
    }

    @Test
    void 지원하지_않는_형식의_이미지는_검증할_수_없다() throws IOException {
        // given
        byte[] gifImage = createImage("gif", 1, 1);

        // when & then
        assertImageError(gifImage, AccountErrorCode.UNSUPPORTED_PROFILE_IMAGE_FORMAT);
    }

    @Test
    void 허용_해상도를_초과한_이미지는_검증할_수_없다() throws IOException {
        // given
        byte[] oversizedImage = createImage("png", 4097, 1);

        // when & then
        assertImageError(oversizedImage, AccountErrorCode.PROFILE_IMAGE_DIMENSION_EXCEEDED);
    }

    private void assertImageError(
            byte[] content,
            AccountErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(() -> profileImageValidator.validateAndGetContentType(content))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode)
                );
    }

    private byte[] createImage(
            String format,
            int width,
            int height
    ) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);
        return outputStream.toByteArray();
    }
}
