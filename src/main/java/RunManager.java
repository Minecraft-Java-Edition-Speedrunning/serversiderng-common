import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

interface YggdrasilAuthenticator {

}

record YggdrasilAuthentication(String srcApiKey, YggdrasilAuthenticator yggdrasilAuthenticator, String mcsrApiKey) {}

class AuthenticationFactory {
    private String srcApiKey;
    private YggdrasilAuthenticator yggdrasilAuthenticator;
    private String mcsrApiKey;

    public void setSrcApiKey(String srcApiKey) {
        this.srcApiKey = srcApiKey;
    }

    public void setYggdrasilAuthenticator(YggdrasilAuthenticator yggdrasilAuthenticator) {
        this.yggdrasilAuthenticator = yggdrasilAuthenticator;
    }

    public void setMcsrApiKey(String mcsrApiKey) {
        this.mcsrApiKey = mcsrApiKey;
    }

    public YggdrasilAuthentication build() {
        return new YggdrasilAuthentication(srcApiKey, yggdrasilAuthenticator, mcsrApiKey);
    }
}

record AccessRefreshToken (
    String refreshToken,
    String accessToken,
    LocalDateTime expiresAt
) {}

class AccessRefreshTokenProvider {
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
        }
        else {
            authenticate.accept(this);
        }
    }

    private void authenticateYggdrasil(YggdrasilAuthentication authentication) {
        AccessRefreshToken accessRefreshToken = serverClient.authenticate(
                authentication.srcApiKey(),
                authentication.yggdrasilAuthenticator(),
                authentication.mcsrApiKey()
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
        while(!isPresent()) {
            this.refresh();
        }
        return consumer.apply(accessToken);
    }
}

record TokenResponse<T> (
    T data,
    String token
) {}

record RunStart (
    String instance,
    Boolean isSetSeed,
    String seed,
    String randomSalt,
    String randomSourceId,
    String runId,
    Instant startTime,
    Long blockSize
) {}

record RandomBlock (
    String runId,
    Long block,
    String seed,
    String hashAlgorithm
) {}

record Timebox (
    String runId,
    String cause
) {}

interface ServerInterface {
    AccessRefreshToken authenticate(String srcApiKey, YggdrasilAuthenticator yggdrasilAuthenticator, String mcsrApiKey);
    AccessRefreshToken refresh(String refreshToken);
    TokenResponse<RunStart> startRun(
        String authorization,
        String seed
    );
    TokenResponse<RandomBlock> getRandom(
        String authorization,
        String runToken,
        Long block
    );
    TokenResponse<Timebox> timeboxRun(
            String authorization,
            String runToken,
            String hash,
            String cause
    );
}

class ServerClient {
    private final ServerInterface serverInterface;
    private final AccessRefreshTokenProvider accessRefreshToken;

    public ServerClient(RefreshTokenStore refreshTokenStore, YggdrasilAuthentication authentication) {
        // TODO: figure out how serverClient is getting initiated
        serverInterface = null;
        this.accessRefreshToken = new AccessRefreshTokenProvider(serverInterface, refreshTokenStore, authentication);
    }

    private String getAuthorisation() {
        return this.accessRefreshToken.flatMap(
            (accessToken) -> "Bearer " + accessToken
        );
    }
    public TokenResponse<RunStart> startRun(String seed) {
        return serverInterface.startRun(getAuthorisation(), seed);
    }
    public TokenResponse<RandomBlock> getRandom(String runToken, Long block) {
        return serverInterface.getRandom(getAuthorisation(), runToken, block);
    }
    public TokenResponse<Timebox> timeboxRun(String runToken, String hash, String cause) {
        return serverInterface.timeboxRun(getAuthorisation(), runToken, hash, cause);
    }
}

class RunServerClient {
    String runToken;
    ServerClient serverClient;

    public RunServerClient(ServerClient serverClient, String runToken) {
        this.serverClient = serverClient;
        this.runToken = runToken;
    }
    public TokenResponse<RandomBlock> getRandom(Long block) {
        return serverClient.getRandom(runToken, block);
    }
    public TokenResponse<Timebox> timeboxRun(String hash, String cause) {
        return serverClient.timeboxRun(runToken, hash, cause);
    }
}

interface RandomType {
    int sourceIndex();
}

class GameRandom extends Random {
    public GameRandom(Long seed, Long calls) {
        super(seed);
        for(int i = 0; i < calls; i++){
            this.next(32);
        }
    }
}

class RandomSource<T extends RandomType> {
    public final RandomBlock block;
    private final Map<T, Random> sources;

    public RandomSource(RandomBlock block, Map<T, Long> calls) {
        this.block = block;
        this.sources = new HashMap<>();
        calls.forEach(this::createRandom);
    }

    private Random createRandom(T eventType, Long calls) {
        try {
            byte[] parts = Base64.getDecoder().decode(block.seed());
            MessageDigest digest = MessageDigest.getInstance(block.hashAlgorithm());
            for (byte part : parts) {
                digest.update((byte) (part ^ eventType.sourceIndex()));
            }
            Long seed = ByteBuffer.wrap(digest.digest()).getLong();

            return sources.put(eventType, new GameRandom(seed, calls));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Random getRandom(T eventType) {
        return this.sources.computeIfAbsent(eventType, (type) -> createRandom(type, 0L));
    }
}

record SavedRandom<T extends RandomType> (
    RandomBlock block,
    Map<T, Long> calls
) {}

class Run<T extends RandomType> {
    private final RunStart runStart;
    private RunServerClient serverClient;
    private RandomSource<T> activeSource;

    public Run(ServerClient serverClient, String seed){
        TokenResponse<RunStart> runStart = serverClient.startRun(seed);
        // TODO: log runStart somewhere
        this.runStart = runStart.data();
        initRun(serverClient, runStart.token(), null);
    }

    public Run(ServerClient serverClient, TokenResponse<RunStart> runStart, SavedRandom<T> savedRandom){
        this.runStart = runStart.data();
        initRun(serverClient, runStart.token(), savedRandom);
    }

    private void initRun(ServerClient serverClient, String runToken, SavedRandom<T> savedRandom) {
        this.serverClient = new RunServerClient(serverClient, runToken);
        if (savedRandom != null) {
            this.activeSource = new RandomSource<>(savedRandom.block(), savedRandom.calls());
        }
    }

    private Long getActiveBlockIndex() {
        return Duration.between(runStart.startTime(), Instant.now()).toMillis() / runStart.blockSize();
    }

    private RandomSource<T> getActiveBlock() {
        Long activeBlock = this.getActiveBlockIndex();
        while (!Objects.equals(this.activeSource.block.block(), activeBlock)) {
            TokenResponse<RandomBlock> block = this.serverClient.getRandom(activeBlock);
            // TODO: log block somewhere
            this.activeSource = new RandomSource<>(block.data(), new HashMap<>());
            activeBlock = this.getActiveBlockIndex();
        }
        return this.activeSource;
    }

    public Random getRandom(T eventType) {
        RandomSource<T> randomSource = this.getActiveBlock();
        return randomSource.getRandom(eventType);
    }

    public void timebox(String hash, String cause) {
        TokenResponse<Timebox> timebox = this.serverClient.timeboxRun(hash, cause);
        // TODO: log timebox somewhere
    }

}

interface RefreshTokenStore {
    void saveRefreshToken(String refreshToken);
    String getRefreshToken();
}

public class RunManager<T extends RandomType> {
    private final ServerClient serverClient;

    public RunManager(RefreshTokenStore refreshTokenStore, YggdrasilAuthentication authentication) {
        this.serverClient = new ServerClient(refreshTokenStore, authentication);
    }

    Run<T> startRun(String seed) {
        return new Run<>(this.serverClient, seed);
    }

    Run<T> loadRun(TokenResponse<RunStart> runStart, SavedRandom<T> savedRandom) {
        return new Run<>(this.serverClient, runStart, savedRandom);
    }
}
