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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Run<T extends RandomType> {
    private final RunStart runStart;
    private RunServerClient serverClient;
    private EventTokenLogger eventTokenLogger;
    private String runKey;
    private RandomSource<T> activeSource;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private TokenResponse<RandomBlock> nextBlock;

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

    private Long getActiveBlockIndex() {
        return Duration.between(Instant.ofEpochMilli(runStart.startTime()), Instant.now()).toMillis() / runStart.blockSize();
    }

    private TokenResponse<RandomBlock> fetchBlock(Long block) {
        TokenResponse<RandomBlock> randomBlock = this.serverClient.getRandom(block);
        this.eventTokenLogger.randomBlock(runKey, randomBlock);
        return randomBlock;
    }

    private TokenResponse<RandomBlock> getBlock(Long block) {
        if (this.nextBlock != null && Objects.equals(this.nextBlock.data().block(), block)) {
            return this.nextBlock;
        }
        return fetchBlock(block);
    }

    private void initRun(ServerClient serverClient, EventTokenLogger eventTokenLogger, String runToken, SavedRandom<T> savedRandom, String runKey) {
        this.serverClient = new RunServerClient(serverClient, runToken);
        this.eventTokenLogger = eventTokenLogger;
        this.runKey = runKey;
        if (savedRandom != null) {
            this.activeSource = new RandomSource<>(savedRandom.block(), savedRandom.calls());
        }
        long initDelay = runStart.blockSize() - (Duration.between(Instant.ofEpochMilli(runStart.startTime()), Instant.now()).toMillis() % runStart.blockSize());
        scheduler.scheduleAtFixedRate(() -> {
            long nextBlock = getActiveBlockIndex() + 1;
            this.nextBlock = fetchBlock(nextBlock);
        }, initDelay, runStart.blockSize(), TimeUnit.MILLISECONDS);
    }

    private RandomSource<T> getActiveBlock() {
        Long activeBlock = this.getActiveBlockIndex();
        while (this.activeSource == null || !Objects.equals(this.activeSource.block.block(), activeBlock)) {
            TokenResponse<RandomBlock> block = getBlock(activeBlock);
            this.activeSource = new RandomSource<>(block.data(), new HashMap<>());
            activeBlock = this.getActiveBlockIndex();
        }
        return this.activeSource;
    }

    @SuppressWarnings("unused")
    public Random getRandom(T eventType) {
        RandomSource<T> randomSource = this.getActiveBlock();
        return randomSource.getRandom(eventType);
    }

    @SuppressWarnings("unused")
    public TokenResponse<Timebox> timebox(String hash, String cause) {
        return this.serverClient.timeboxRun(hash, cause);
    }

    @SuppressWarnings("unused")
    public SavedRandom<T> exportRandom() {
        return new SavedRandom<>(
            activeSource.block,
            activeSource.exportCalls()
        );
    }
}
