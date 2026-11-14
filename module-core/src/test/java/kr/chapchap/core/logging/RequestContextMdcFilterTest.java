package kr.chapchap.core.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextMdcFilterTest {

    private final RequestContextMdcFilter filter = new RequestContextMdcFilter();

    @Test
    void 필터_실행_중에는_MDC에_메서드와_URI가_담긴다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/places/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then (체인 실행 중 MDC 값 확인)
        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get(RequestContextMdcFilter.REQUEST_CONTEXT_MDC_KEY))
                        .isEqualTo("GET /api/places/123"));
    }

    @Test
    void 쿼리스트링이_있으면_MDC_값에도_포함된다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/places/123/photos");
        request.setQueryString("lang=ko");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get(RequestContextMdcFilter.REQUEST_CONTEXT_MDC_KEY))
                        .isEqualTo("GET /api/places/123/photos?lang=ko"));
    }

    @Test
    void 필터_실행이_끝나면_MDC가_정리된다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/places");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> { });

        // then
        assertThat(MDC.get(RequestContextMdcFilter.REQUEST_CONTEXT_MDC_KEY)).isNull();
    }
}
