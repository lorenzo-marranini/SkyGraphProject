package it.unipi.SkyGraph.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;

/**
 * Utility class providing stateless JWT operations: token generation, validation,
 * and claim extraction. The HMAC-SHA256 signing key is generated once at class
 * loading time and held in memory for the lifetime of the application.
 */
public class JwtUtils {
    private static final String secretKey;
    private static final long JWT_TOKEN_VALIDITY = 2 * 60 * 60 * 1000; // 2 ore

    static {

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGen.generateKey();
            secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private JwtUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a signed JWT for the given subject and roles.
     * The token embeds a {@code roles} claim and expires after {@link #JWT_TOKEN_VALIDITY}.
     *
     * @param sub   the subject (typically a user ID)
     * @param roles list of role strings to embed as the {@code roles} claim
     * @return compact, URL-safe JWT string
     */
    public static String generateToken(String sub, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        return Jwts.builder()
                .claims(claims)
                .subject(sub)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getKey())
                .compact();
    }

    public static List<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }
    public static boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private static SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private static Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private static Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}