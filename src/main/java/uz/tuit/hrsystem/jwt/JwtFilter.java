package uz.tuit.hrsystem.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.tuit.hrsystem.entity.Token;
import uz.tuit.hrsystem.repository.UserRepository;
import uz.tuit.hrsystem.repository.TokenRepository;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {


    public static String getphoneNumber;
    public static String getEmail;
    public static String getToken;
    public static String getRole;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    UserRepository userRepository;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if(token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Optional<Token> optionalToken = tokenRepository.findByToken(token);
            if (jwtProvider.validateToken(token) && optionalToken.isPresent()) {
                getEmail = jwtProvider.getUsername(token);
                getToken = token;
                getRole = jwtProvider.getAuthentication(token).getName();
                Authentication authentication =
                        optionalToken.get().getLevel().equals("TEMPORARY") ?
                                jwtProvider.getAuthentication2(token) :
                                jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}