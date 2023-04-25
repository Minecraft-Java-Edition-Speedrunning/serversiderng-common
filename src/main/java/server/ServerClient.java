package server;


import auth.AccessRefreshTokenProvider;
import client.RefreshTokenStore;
import auth.YggdrasilAuthentication;
import model.*;

public class ServerClient {
    private final ServerInterface serverInterface;
    private final AccessRefreshTokenProvider accessRefreshToken;

    public ServerClient(String host, RefreshTokenStore refreshTokenStore, YggdrasilAuthentication authentication) {
        serverInterface = new ServerInterface(host);
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
    public TokenResponse<RandomBlock> getRandom(RunForm<RandomBlockForm> randomBlockForm) {
        return serverInterface.getRandom(getAuthorisation(), randomBlockForm);
    }
    public TokenResponse<Timebox> timeboxRun(RunForm<TimeboxForm> timeboxForm) {
        return serverInterface.timeboxRun(getAuthorisation(), timeboxForm);
    }
}