package com.fixora.config;

import com.fixora.entity.User;
import com.fixora.enums.UserRole;
import com.fixora.exception.UnauthorizedActionException;
import com.fixora.repository.UserRepository;
import com.fixora.validation.CurrentUserId;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@code @CurrentUserId Long userId} from the X-User-Id header.
 *
 * No Spring Security / JWT is used in this first version, so the header IS the
 * "session". Everything is re-validated against the database downstream.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = annotation == null || annotation.required();
        boolean adminOnly = annotation != null && annotation.adminOnly();

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = request == null ? null : request.getHeader(USER_HEADER);

        if (header == null || header.isBlank()) {
            if (required) {
                throw new UnauthorizedActionException(
                        "No user identified. Sign in first (the frontend sends the X-User-Id header).");
            }
            return null;
        }

        long userId;
        try {
            userId = Long.parseLong(header.trim());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedActionException("Malformed " + USER_HEADER + " header.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedActionException("Unknown user id " + userId + "."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedActionException("This account is deactivated.");
        }

        if (adminOnly && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedActionException("Administrator access is required for this action.");
        }

        return user.getId();
    }
}
