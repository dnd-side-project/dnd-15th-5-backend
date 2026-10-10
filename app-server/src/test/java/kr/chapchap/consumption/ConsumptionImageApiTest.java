package kr.chapchap.consumption;

import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.consumption.api.controller.ConsumptionImageController;
import kr.chapchap.consumption.application.command.ConsumptionImageUploadCommand;
import kr.chapchap.consumption.application.info.ConsumptionImageUploadInfo;
import kr.chapchap.consumption.application.service.ConsumptionImageUploadService;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsumptionImageController.class)
@Import({SecurityConfig.class, CorsConfig.class, WebMvcConfig.class, GlobalExceptionHandler.class})
class ConsumptionImageApiTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ConsumptionImageUploadService upload;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void 사진을_업로드하면_ID와_만료_시각을_반환한다() throws Exception {
        // given
        byte[] content = {1, 2, 3};
        given(upload.upload(any())).willReturn(new ConsumptionImageUploadInfo(42L, LocalDateTime.of(2026, 10, 11, 12, 0)));
        // when
        mvc.perform(multipart("/v1/consumptions/images")
                        .file(new MockMultipartFile("image", "photo.png", "image/png", content))
                        .with(jwt().jwt(token -> token.subject("1")).authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageId").value(42))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-10-11T12:00:00"));
        // then
        var captor = ArgumentCaptor.forClass(ConsumptionImageUploadCommand.class);
        then(upload).should().upload(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().content()).containsExactly(content);
    }

    @Test
    void 파일이_없으면_거절한다() throws Exception {
        // when & then
        mvc.perform(multipart("/v1/consumptions/images")
                        .with(jwt().jwt(token -> token.subject("1")).authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isBadRequest());
        then(upload).shouldHaveNoInteractions();
    }

    @Test
    void 인증하지_않으면_업로드할_수_없다() throws Exception {
        // when & then
        mvc.perform(multipart("/v1/consumptions/images")
                        .file(new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1})))
                .andExpect(status().isUnauthorized());
        then(upload).shouldHaveNoInteractions();
    }
}
