package auth;

import server.ServerInterface;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccessRefreshTokenProvider {
    private final ServerInterface serverClient;
    private final RefreshTokenStore refreshTokenStore;
    private String refreshToken;
    private String accessToken;
    private LocalDateTime expiresAt;

    public AccessRefreshTokenProvider(ServerInterface serverClient, RefreshTokenStore refreshTokenStore, YggdrasilAuthentication authentication) {
        this(
                serverClient,
                refreshTokenStore,
                (AccessRefreshTokenProvider self) -> self.authenticateYggdrasil(authentication)
        );
    }

    private AccessRefreshTokenProvider(ServerInterface serverClient, RefreshTokenStore refreshTokenStore, Consumer<AccessRefreshTokenProvider> authenticate) {
        this.serverClient = serverClient;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshToken = refreshTokenStore.getRefreshToken();
        if (this.refreshToken != null) {
            refresh();
            // TODO: error handling if refresh token is invalidated at this point
//            authenticate.accept(this);
        } else {
            authenticate.accept(this);
        }
    }

    private void authenticateYggdrasil(YggdrasilAuthentication authentication) {
        AccessRefreshToken accessRefreshToken = serverClient.authenticateYggdrasil(
                authentication.uuid(),
                authentication.challenge(),
                authentication.response(),
                authentication.publicKey(),
                authentication.signature(),
                authentication.instant()
        );
        this.applyTokens(accessRefreshToken);
    }

    private void refresh() {
        assert refreshToken != null;
        AccessRefreshToken accessRefreshToken = serverClient.refresh(refreshToken);
        this.applyTokens(accessRefreshToken);
    }

    private void applyTokens(AccessRefreshToken tokens) {
        this.refreshToken = tokens.refreshToken();
        this.accessToken = tokens.accessToken();
        this.expiresAt = tokens.expiresAt();
        this.refreshTokenStore.saveRefreshToken(this.refreshToken);
        // TODO: schedule auto refresh
    }

    public boolean isPresent() {
        return accessToken != null && expiresAt.isAfter(LocalDateTime.now());
    }

    public <T> T flatMap(Function<String, T> consumer) {
        while (!isPresent()) {
            this.refresh();
        }
        return consumer.apply(accessToken);
    }
}
