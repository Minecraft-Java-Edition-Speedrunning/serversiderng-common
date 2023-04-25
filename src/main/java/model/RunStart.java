package model;

public record RunStart (
        String instance,
        Boolean isSetSeed,
        String seed,
        String randomSalt,
        Long randomSourceId,
        String runId,
        Long startTime,
        Long blockSize
) {}
