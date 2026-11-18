package com.fpt.ems.be.security;

import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = jwtUtils.extractToken(request);

        if (token != null) {
            try {
                Claims claims = jwtUtils.parseAndValidateClaims(token);
                Long userId = Long.parseLong(claims.getSubject());

                // Zero-trust token claims: Always fetch fresh user & role from DB on every request
                Optional<AppUser> userOpt = appUserRepository.findById(userId);
                if (userOpt.isEmpty()) {
                    log.warn("JWT rejected: User ID {} from sub claim not found in database", userId);
                    request.setAttribute("JWT_ERROR_REASON", "Người dùng không tồn tại trong hệ thống");
                } else {
                    AppUser user = userOpt.get();

                    if (user.getStatus() != UserStatus.ACTIVE) {
                        log.warn("JWT rejected: User {} is LOCKED / INACTIVE in database", user.getUsername());
                        request.setAttribute("JWT_ERROR_REASON", "Tài khoản đã bị khóa hoặc ngừng hoạt động");
                    } else if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
                        log.warn("JWT rejected: User {} is temporarily locked until {}", user.getUsername(), user.getLockedUntil());
                        request.setAttribute("JWT_ERROR_REASON", "Tài khoản đang bị tạm khóa đến " + user.getLockedUntil());
                    } else {
                        // User is active: populate UserPrincipal using fresh DB role & authorities
                        UserPrincipal principal = UserPrincipal.create(user);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ex) {
                log.warn("JWT verification failed on {}: {}", request.getRequestURI(), ex.getMessage());
                request.setAttribute("JWT_ERROR_REASON", "Token không hợp lệ, sai chữ ký hoặc đã hết hạn");
            }
        }

        filterChain.doFilter(request, response);
    }
}

