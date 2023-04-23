package auth;

import java.time.LocalDateTime;

public record AccessRefreshToken(
        String refreshToken,
        String accessToken,
        LocalDateTime expiresAt
) {
}
