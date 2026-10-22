package com.northbank.customerservice.security;

import com.northbank.customerservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(JwtAuthFilter.JWT_ERROR_ATTRIBUTE);

        String message;
        if (JwtAuthFilter.ERROR_EXPIRED.equals(reason)) {
            message = "Token has expired. Please log in again.";
        } else if (JwtAuthFilter.ERROR_INVALID.equals(reason)) {
            message = "Invalid authentication token.";
        } else {
            message = "Authentication required. Please provide a valid token.";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                message,
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
