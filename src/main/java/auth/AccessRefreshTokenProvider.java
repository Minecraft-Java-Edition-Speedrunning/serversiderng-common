package auth;

import client.RefreshTokenStore;
import server.ServerInterface;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccessRefreshTokenProvider {

    private static final Long PREFETCH_MILLISECONDS = (long) 30 * 1000;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledRefresh;
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
        this.scheduler = Executors.newScheduledThreadPool(1);
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

        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(false);
        }
        scheduledRefresh = scheduler.schedule(
                this::refresh,
                Math.max(0, Duration.between(this.expiresAt, Instant.now()).toMillis() - PREFETCH_MILLISECONDS),
                TimeUnit.MILLISECONDS
        );
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
