package auth;

import java.time.Instant;

public record AccessRefreshToken(
        String refreshToken,
        String accessToken,
        Instant expiresAt
) {
}
