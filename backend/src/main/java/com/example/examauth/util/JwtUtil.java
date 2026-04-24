package com.example.examauth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.example.examauth.service.SettingsService;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "examhub_secret_key"; // change this later
    private final SettingsService settingsService;

    public JwtUtil(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Generate a JWT that embeds the user's current tokenVersion.
     * When tokenVersion is incremented (on password/email change),
     * all previously issued tokens become invalid.
     */
    public String generateToken(String email) {
        return generateToken(email, 0);
    }

    public String generateToken(String email, int tokenVersion) {
        int sessionMinutes = settingsService.getIntSetting(
                SettingsService.KEY_SESSION_TIMEOUT_MINUTES,
                15);
        return Jwts.builder()
                .setSubject(email)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (sessionMinutes * 60L * 1000L)))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extract the tokenVersion claim embedded in the JWT.
     * Returns 0 if the claim is absent (backwards-compatible with old tokens).
     */
    public int extractTokenVersion(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            Object v = claims.get("tokenVersion");
            return v != null ? ((Number) v).intValue() : 0;
        } catch (Exception e) {
            return -1; // invalid token
        }
    }

    public boolean validateToken(String token, String email) {
        try {
            final String extractedEmail = extractUsername(token);
            return (extractedEmail.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
}
