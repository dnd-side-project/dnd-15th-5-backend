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
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RequestContextMdcFilter extends OncePerRequestFilter {

    static final String REQUEST_CONTEXT_MDC_KEY = "requestContext";

    private static final Set<String> SENSITIVE_QUERY_PARAM_NAMES = Set.of(
            "code", "state", "token", "access_token", "refresh_token", "id_token",
            "client_secret", "password", "secret"
    );

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
        String base = request.getMethod() + " " + request.getRequestURI();
        String queryString = request.getQueryString();
        return StringUtils.hasText(queryString) ? base + "?" + maskSensitiveParams(queryString) : base;
    }

    private String maskSensitiveParams(String queryString) {
        return Arrays.stream(queryString.split("&"))
                .map(this::maskIfSensitive)
                .collect(Collectors.joining("&"));
    }

    private String maskIfSensitive(String nameValuePair) {
        int separatorIndex = nameValuePair.indexOf('=');
        String name = separatorIndex >= 0 ? nameValuePair.substring(0, separatorIndex) : nameValuePair;
        if (!SENSITIVE_QUERY_PARAM_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            return nameValuePair;
        }
        return name + "=***";
    }
}
