package it.unipi.SkyGraph.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {


    /**
     * Extracts and validates the JWT from the {@code Authorization: Bearer <token>} header.
     * If the token is valid and no authentication is already present in the security context,
     * builds a {@link UsernamePasswordAuthenticationToken} with the user ID and roles
     * extracted from the token claims and registers it in the {@link SecurityContextHolder}.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to execute after this filter
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String userId = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            userId = JwtUtils.extractUserId(token);
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (JwtUtils.validateToken(token)) {
                // 1. Estraiamo la lista di stringhe (es. ["ADMIN", "AIRLINE_REP"])
                List<String> rawAuthorities = JwtUtils.extractAuthorities(token);

                // 2. Le convertiamo in SimpleGrantedAuthority
                List<SimpleGrantedAuthority> authorities = rawAuthorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // 3. Creiamo un oggetto User standard di Spring (più leggero di UserMongo)
                UserDetails userDetails = new User(userId, "", authorities);
                // 4. Creiamo il token di autenticazione passando le authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}