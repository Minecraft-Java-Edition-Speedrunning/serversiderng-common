package auth;

public interface RefreshTokenStore {
    void saveRefreshToken(String refreshToken);

    String getRefreshToken();
}
