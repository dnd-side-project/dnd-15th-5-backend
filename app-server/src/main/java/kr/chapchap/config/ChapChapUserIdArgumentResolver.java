package kr.chapchap.config;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import kr.chapchap.core.web.auth.ChapChapUserId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;

public class ChapChapUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> parameterType = parameter.getParameterType();
        return parameter.hasParameterAnnotation(ChapChapUserId.class)
                && (parameterType == Long.class || parameterType == long.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Principal principal = webRequest.getUserPrincipal();
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                    exception
            );
        }
    }
}
