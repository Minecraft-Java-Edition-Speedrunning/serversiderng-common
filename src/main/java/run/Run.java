package run;

import client.RandomType;
import model.RandomBlock;
import model.RunStart;
import model.Timebox;
import model.TokenResponse;
import server.RunServerClient;
import server.ServerClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class Run<T extends RandomType> {
    private final RunStart runStart;
    private RunServerClient serverClient;
    private RandomSource<T> activeSource;

    public Run(ServerClient serverClient, String seed) {
        TokenResponse<RunStart> runStart = serverClient.startRun(seed);
        // TODO: log runStart somewhere
        this.runStart = runStart.data();
        initRun(serverClient, runStart.token(), null);
    }

    public Run(ServerClient serverClient, TokenResponse<RunStart> runStart, SavedRandom<T> savedRandom) {
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
