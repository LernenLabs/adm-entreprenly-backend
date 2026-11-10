package online.entreprenly.platform.iam.infrastructure.tokens.jwt.services;

import online.entreprenly.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import online.entreprenly.platform.iam.domain.model.aggregates.User;
import online.entreprenly.platform.iam.domain.repositories.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Token service implementation for JWT tokens.
 * This class is responsible for generating and validating JWT tokens.
 * It uses the secret and expiration days from the application.properties file.
 */
@Service
@Slf4j
public class TokenServiceImpl implements BearerTokenService {
    private static final String AUTHORIZATION_PARAMETER_NAME = "Authorization";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    private static final int TOKEN_BEGIN_INDEX = 7;


    @Value("${authorization.jwt.secret}")
    private String secret;

    @Value("${authorization.jwt.expiration.days}")
    private int expirationDays;

    private final UserRepository users;

    public TokenServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public String generateToken(User user) {
        var issuedAt = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .claim("uid", user.getId())
                .claim("sessionVersion", user.getSessionVersion())
                .issuedAt(issuedAt)
                .expiration(DateUtils.addDays(issuedAt, expirationDays))
                .signWith(getSigningKey())
                .compact();
    }


    /**
     * This method extracts the username from a JWT token
     * @param token the token
     * @return String the username
     */
    @Override
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * This method validates a JWT token
     * @param token the token
     * @return boolean true if the token is valid, false otherwise
     */
    @Override
    public boolean validateToken(String token) {
        try {
            var claims = extractAllClaims(token);
            return users.findByEmail(claims.getSubject())
                    .flatMap(user -> activeSessionId(claims, user)).isPresent();
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public Optional<String> activeSessionId(String token, User user) {
        try {
            return activeSessionId(extractAllClaims(token), user);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<String> activeSessionId(Claims claims, User user) {
        var version = claims.get("sessionVersion", Long.class);
        var id = claims.getId();
        if (id == null || version == null || !user.getEmail().equals(claims.getSubject())
                || !user.getId().equals(claims.get("uid", Long.class))) {
            return Optional.empty();
        }
        return version == user.getSessionVersion() || id.equals(user.getRetainedSessionId())
                ? Optional.of(id) : Optional.empty();
    }


    /**
     * Extract a claim from a token
     * @param token the token
     * @param claimsResolvers the claims resolver
     * @param <T> the type of the claim
     * @return T the claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    /**
     * Extract all claims from a token
     * @param token the token
     * @return Claims the claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Get the signing key
     * @return SecretKey the signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isTokenPresentIn(String authorizationParameter) {
        return StringUtils.hasText(authorizationParameter);
    }

    private boolean isBearerTokenIn(String authorizationParameter) {
        return authorizationParameter.startsWith(BEARER_TOKEN_PREFIX);
    }

    private String extractTokenFrom(String authorizationHeaderParameter) {
        return authorizationHeaderParameter.substring(TOKEN_BEGIN_INDEX);
    }

    private String getAuthorizationParameterFrom(HttpServletRequest request) {
        return request.getHeader(AUTHORIZATION_PARAMETER_NAME);
    }

    @Override
    public String getBearerTokenFrom(HttpServletRequest request) {
        String parameter = getAuthorizationParameterFrom(request);
        if (isTokenPresentIn(parameter) && isBearerTokenIn(parameter)) return extractTokenFrom(parameter);
        return null;
    }

}
