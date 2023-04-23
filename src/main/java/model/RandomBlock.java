package model;

public record RandomBlock (
        String runId,
        Long block,
        String seed,
        String hashAlgorithm
) {}
