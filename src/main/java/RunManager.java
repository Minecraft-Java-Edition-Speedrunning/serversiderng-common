import auth.RefreshTokenStore;
import auth.YggdrasilAuthentication;
import client.RandomType;
import model.RunStart;
import model.TokenResponse;
import run.Run;
import run.SavedRandom;
import server.ServerClient;

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
