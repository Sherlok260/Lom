package uz.tuit.hrsystem.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private String secret = "qwertyuiopasdfghjklzxcvbnm314159265358979323846264qwertyuiopasdfghjklzxcvbnm314159265358979323846264";
    private byte[] keyByte = Decoders.BASE64.decode(secret);
    private Key key = Keys.hmacShaKeyFor(keyByte);
    private long expTime = 1000 * 3600;

    private String role = "ROLE_AUTH";


    public String generateToken(Authentication authentication, String token_type) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        return Jwts
                .builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() + (token_type.equals("ACCESS_TOKEN")?expTime*24:expTime*24*7)))
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .compact();
    }

    public String generateToken(String email, Collection<? extends GrantedAuthority> grantedAuthorities, String token_type) {
        String authorities = grantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        return Jwts
                .builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() + (token_type.equals("ACCESS_TOKEN")?expTime*24:expTime*24*7)))
                .setSubject(email)
                .claim("auth", authorities)
                .compact();
    }


    public String generateTokenForVerify(String username) {
        String authorities = role;
        return Jwts
                .builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() + expTime/6))
                .setSubject(username)
                .claim("auth", authorities)
                .compact();
    }

    public boolean validateToken(String token) {
        DecodedJWT jwt = JWT.decode(token);
        if( jwt.getExpiresAt().before(new Date())) {
            System.out.println("token is expired");
            return false;
        }
        System.out.println(jwt.getExpiresAt() + " and " + new Date());
        return true;
    }

//    public boolean validateToken(String token) {
//        try {
//            Jwts
//                    .parserBuilder()
//                    .setSigningKey(key)
//                    .build()
//                    .parseClaimsJws(token);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public String getUsername(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Authentication getAuthentication(String jwt) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get("auth").toString().split(","))
                .filter(auth -> !auth.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        User principal = new User(claims.getSubject(), "", authorities);
        return  new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    public Authentication getAuthentication2(String jwt) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        SimpleGrantedAuthority authorities = new SimpleGrantedAuthority(role);
        User principal = new User(claims.getSubject(), "", List.of(authorities));
        return  new UsernamePasswordAuthenticationToken(principal, jwt, List.of(authorities));
    }
}