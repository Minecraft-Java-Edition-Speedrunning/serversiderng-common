package auth;

public record AccessRefreshToken(
        String refreshToken,
        String accessToken,
        Long expiresAt
) { }
