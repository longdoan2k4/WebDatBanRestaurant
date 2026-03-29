package com.example.restaurantpro.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (isApiRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            String path = request.getRequestURI();
            String message = "Ban khong co quyen truy cap tai nguyen nay.";
            String json = "{\"status\":403,\"error\":\"FORBIDDEN\",\"message\":\""
                    + message
                    + "\",\"path\":\""
                    + path
                    + "\"}";
            response.getWriter().write(json);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/403");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");

        return uri.startsWith("/api/")
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
