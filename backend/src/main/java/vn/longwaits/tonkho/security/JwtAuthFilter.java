package vn.longwaits.tonkho.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.longwaits.tonkho.repository.UserRepository;

import java.io.IOException;
import java.util.List;

/** Doc token "Bearer ..." va gan nguoi dung vao phien xu ly. Khong dang ky la bean de tranh chay 2 lan. */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String userId = jwtService.parseUserId(token);
                userRepository.findById(userId)
                        .filter(u -> u.isActive())
                        .ifPresent(u -> {
                            AuthUser principal = new AuthUser(u.getId(), u.getUsername(), u.getFullName(), u.getRole());
                            var authority = new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase());
                            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (Exception ignored) {
                // token sai hoac het han: de nguyen trang thai chua dang nhap
            }
        }
        chain.doFilter(request, response);
    }
}
