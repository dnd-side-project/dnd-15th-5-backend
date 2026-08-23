package kr.chapchap.consumption;

import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.consumption.api.controller.VisitedPlaceSearchController;
import kr.chapchap.consumption.application.command.VisitedPlaceSearchCommand;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo.VisitedPlaceInfo;
import kr.chapchap.consumption.application.service.VisitedPlaceSearchService;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        CorsConfig.class,
        WebMvcConfig.class,
        GlobalExceptionHandler.class
})
@WebMvcTest(VisitedPlaceSearchController.class)
class VisitedPlaceSearchApiTest {

    private static final Long USER_ID = 1L;
    private static final String KEYWORD = "카페";
    private static final String THUMBNAIL_URL = "https://lh3.googleusercontent.com/place-photo";

    private final MockMvc mockMvc;

    @MockitoBean
    private VisitedPlaceSearchService visitedPlaceSearchService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    VisitedPlaceSearchApiTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void Access_Token이_없으면_방문_장소를_검색할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(get("/places/visited/search")
                        .param("keyword", KEYWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(visitedPlaceSearchService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_방문_장소를_검색할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(get("/places/visited/search")
                        .param("keyword", KEYWORD)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(visitedPlaceSearchService).shouldHaveNoInteractions();
    }

    @Test
    void user_scope로_방문_장소를_검색하면_기본_5개와_사진_URL을_반환한다() throws Exception {
        // given
        given(visitedPlaceSearchService.search(any(VisitedPlaceSearchCommand.class)))
                .willReturn(searchInfo());

        // when & then
        mockMvc.perform(get("/places/visited/search")
                        .param("keyword", "  " + KEYWORD + "  ")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.places[0].placeId").value(11L))
                .andExpect(jsonPath("$.data.places[0].placeName").value("찹찹 카페"))
                .andExpect(jsonPath("$.data.places[0].roadAddress").value("서울특별시 강남구"))
                .andExpect(jsonPath("$.data.places[0].thumbnailUrl").value(THUMBNAIL_URL))
                .andExpect(jsonPath("$.data.places[0].googleMapsUri")
                        .value("https://maps.google.com/photo"))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));

        ArgumentCaptor<VisitedPlaceSearchCommand> commandCaptor =
                ArgumentCaptor.forClass(VisitedPlaceSearchCommand.class);
        then(visitedPlaceSearchService).should().search(commandCaptor.capture());
        VisitedPlaceSearchCommand command = commandCaptor.getValue();
        assertThat(command.userId()).isEqualTo(USER_ID);
        assertThat(command.keyword()).isEqualTo(KEYWORD);
        assertThat(command.cursor()).isNull();
        assertThat(command.size()).isEqualTo(5);
    }

    private VisitedPlaceSearchInfo searchInfo() {
        return new VisitedPlaceSearchInfo(
                List.of(new VisitedPlaceInfo(
                        11L,
                        "찹찹 카페",
                        "서울특별시 강남구",
                        THUMBNAIL_URL,
                        "https://maps.google.com/photo"
                )),
                true,
                "next-cursor"
        );
    }
}
