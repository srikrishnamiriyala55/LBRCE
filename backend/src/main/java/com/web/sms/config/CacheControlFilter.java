package com.web.sms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Prevents authenticated API data and the SPA shell from surviving deployments in browser caches. */
@Component
public class CacheControlFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String accept = request.getHeader("Accept");
        boolean apiRequest = path.startsWith("/api/");
        boolean spaDocument = !path.startsWith("/assets/")
                && accept != null && accept.contains("text/html");

        if (apiRequest || spaDocument) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; base-uri 'self'; frame-ancestors 'none'; object-src 'none'; " +
                "script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; " +
                "font-src 'self' data:; connect-src 'self'");

        filterChain.doFilter(request, response);
    }
}
