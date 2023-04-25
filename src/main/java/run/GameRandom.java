package run;

import java.util.Random;

public class GameRandom extends Random {

    private Long calls = 0L;

    public GameRandom(Long seed, Long calls) {
        super(seed);
        for(int i = 0; i < calls; i++){
            this.next(32);
        }
    }

    @Override
    protected int next(int bits) {
        calls++;
        return super.next(bits);
    }

    public Long getCalls() {
        return calls;
    }
}

