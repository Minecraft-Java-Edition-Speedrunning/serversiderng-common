package run;

import java.util.Random;

public class GameRandom extends Random {
    public GameRandom(Long seed, Long calls) {
        super(seed);
        for(int i = 0; i < calls; i++){
            this.next(32);
        }
    }
}

