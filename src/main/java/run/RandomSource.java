package run;

import client.RandomType;
import model.RandomBlock;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomSource<T extends RandomType> {
    public final RandomBlock block;
    private final Map<T, Random> sources;

    public RandomSource(RandomBlock block, Map<T, Long> calls) {
        this.block = block;
        this.sources = new HashMap<>();
        calls.forEach(this::createRandom);
    }

    private Random createRandom(T eventType, Long calls) {
        try {
            byte[] parts = Base64.getDecoder().decode(block.seed());
            MessageDigest digest = MessageDigest.getInstance(block.hashAlgorithm());
            for (byte part : parts) {
                digest.update((byte) (part ^ eventType.sourceIndex()));
            }
            Long seed = ByteBuffer.wrap(digest.digest()).getLong();

            return sources.put(eventType, new GameRandom(seed, calls));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Random getRandom(T eventType) {
        return this.sources.computeIfAbsent(eventType, (type) -> createRandom(type, 0L));
    }
}
