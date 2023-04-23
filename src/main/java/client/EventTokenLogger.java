package client;

import model.RandomBlock;
import model.RunStart;
import model.TokenResponse;

public interface EventTokenLogger {
    void startRun(String runKey, TokenResponse<RunStart> runStart);
    void randomBlock(String runKey, TokenResponse<RandomBlock> randomBlock);
}
