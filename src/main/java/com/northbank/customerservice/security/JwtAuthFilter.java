package com.northbank.customerservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";
    public static final String ERROR_EXPIRED = "expired";
    public static final String ERROR_INVALID = "invalid";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

//        if (jwtService.isTokenValid(token)) {
//            UUID customerId = jwtService.extractCustomerId(token);
//            String role = jwtService.extractRole(token);
//
//            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                    customerId,
//                    null,
//                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
//            );
//
//            SecurityContextHolder.getContext().setAuthentication(authToken);
//        }
        try {
            Claims claims = jwtService.verifyToken(token);

            UUID customerId = UUID.fromString(claims.getSubject());
            String role = claims.get("role", String.class);

            if (role == null || role.isBlank()) {
                log.warn("Rejected token with missing role claim from {}", request.getRemoteAddr());
                request.setAttribute(JWT_ERROR_ATTRIBUTE, ERROR_INVALID);
            } else {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        customerId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (ExpiredJwtException e) {
            log.info("Expired token presented for {} {}", request.getMethod(), request.getRequestURI());
            request.setAttribute(JWT_ERROR_ATTRIBUTE, ERROR_EXPIRED);

        } catch (SignatureException e) {
            log.warn("Token with invalid signature from {} for {} {}",
                    request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
            request.setAttribute(JWT_ERROR_ATTRIBUTE, ERROR_INVALID);

        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Malformed or unsupported token from {}", request.getRemoteAddr());
            request.setAttribute(JWT_ERROR_ATTRIBUTE, ERROR_INVALID);

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Unusable token presented: {}", e.getClass().getSimpleName());
            request.setAttribute(JWT_ERROR_ATTRIBUTE, ERROR_INVALID);
        }

        filterChain.doFilter(request, response);
    }
}
