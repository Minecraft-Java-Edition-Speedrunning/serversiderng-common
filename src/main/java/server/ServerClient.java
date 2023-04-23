package server;


import auth.AccessRefreshTokenProvider;
import auth.RefreshTokenStore;
import auth.YggdrasilAuthentication;
import model.RandomBlock;
import model.RunStart;
import model.Timebox;
import model.TokenResponse;

public class ServerClient {
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