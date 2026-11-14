package kr.chapchap.core.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestContextMdcFilter extends OncePerRequestFilter {

    static final String REQUEST_CONTEXT_MDC_KEY = "requestContext";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            MDC.put(REQUEST_CONTEXT_MDC_KEY, requestContext(request));
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_CONTEXT_MDC_KEY);
        }
    }

    private String requestContext(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return StringUtils.hasText(queryString)
                ? request.getMethod() + " " + request.getRequestURI() + "?" + queryString
                : request.getMethod() + " " + request.getRequestURI();
    }
}
