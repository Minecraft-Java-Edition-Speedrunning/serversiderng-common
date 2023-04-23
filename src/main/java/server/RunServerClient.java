package server;

import model.RandomBlock;
import model.Timebox;
import model.TokenResponse;

public class RunServerClient {
    String runToken;
    ServerClient serverClient;

    public RunServerClient(ServerClient serverClient, String runToken) {
        this.serverClient = serverClient;
        this.runToken = runToken;
    }
    public TokenResponse<RandomBlock> getRandom(Long block) {
        return serverClient.getRandom(runToken, block);
    }
    public TokenResponse<Timebox> timeboxRun(String hash, String cause) {
        return serverClient.timeboxRun(runToken, hash, cause);
    }
}
