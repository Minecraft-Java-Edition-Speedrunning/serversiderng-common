package client;

import auth.YggdrasilAuthentication;
import model.RunStart;
import model.TokenResponse;
import run.Run;
import run.SavedRandom;
import server.ServerClient;

public class RunManager<T extends RandomType> {
    private final ServerClient serverClient;
    private final EventTokenLogger eventTokenLogger;

    public RunManager(RefreshTokenStore refreshTokenStore, EventTokenLogger eventTokenLogger, YggdrasilAuthentication authentication) {
        this.serverClient = new ServerClient(refreshTokenStore, authentication);
        this.eventTokenLogger = eventTokenLogger;
    }

    Run<T> startRun(String seed, String runKey) {
        return new Run<>(this.serverClient, this.eventTokenLogger, seed, runKey);
    }

    Run<T> loadRun(TokenResponse<RunStart> runStart, SavedRandom<T> savedRandom, String runKey) {
        return new Run<>(this.serverClient, this.eventTokenLogger, runStart, savedRandom, runKey);
    }
}
