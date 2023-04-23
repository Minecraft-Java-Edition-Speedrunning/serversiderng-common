package run;

import client.RandomType;
import model.RandomBlock;

import java.util.Map;

public record SavedRandom<T extends RandomType> (
        RandomBlock block,
        Map<T, Long> calls
) {}