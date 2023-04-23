package model;

import java.time.Instant;

public record RunStart (
        String instance,
        Boolean isSetSeed,
        String seed,
        String randomSalt,
        String randomSourceId,
        String runId,
        Instant startTime,
        Long blockSize
) {}
