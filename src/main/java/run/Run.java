package run;

import client.EventTokenLogger;
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
    private EventTokenLogger eventTokenLogger;
    private String runKey;
    private RandomSource<T> activeSource;

    public Run(ServerClient serverClient, EventTokenLogger eventTokenLogger, String seed, String runKey) {
        TokenResponse<RunStart> runStart = serverClient.startRun(seed);
        eventTokenLogger.startRun(runKey, runStart);
        this.runStart = runStart.data();
        initRun(serverClient, eventTokenLogger, runStart.token(), null, runKey);
    }

    public Run(ServerClient serverClient, EventTokenLogger eventTokenLogger, TokenResponse<RunStart> runStart, SavedRandom<T> savedRandom, String runKey) {
        this.runStart = runStart.data();
        initRun(serverClient, eventTokenLogger, runStart.token(), savedRandom, runKey);
    }

    private void initRun(ServerClient serverClient, EventTokenLogger eventTokenLogger, String runToken, SavedRandom<T> savedRandom, String runKey) {
        this.serverClient = new RunServerClient(serverClient, runToken);
        this.eventTokenLogger = eventTokenLogger;
        this.runKey = runKey;
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
            this.eventTokenLogger.randomBlock(runKey, block);
            this.activeSource = new RandomSource<>(block.data(), new HashMap<>());
            activeBlock = this.getActiveBlockIndex();
        }
        return this.activeSource;
    }

    public Random getRandom(T eventType) {
        RandomSource<T> randomSource = this.getActiveBlock();
        return randomSource.getRandom(eventType);
    }

    public TokenResponse<Timebox> timebox(String hash, String cause) {
        return this.serverClient.timeboxRun(hash, cause);
    }
}
