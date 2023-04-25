package server;

import model.*;

public class RunServerClient {
    String runToken;
    ServerClient serverClient;

    public RunServerClient(ServerClient serverClient, String runToken) {
        this.serverClient = serverClient;
        this.runToken = runToken;
    }
    public TokenResponse<RandomBlock> getRandom(Long block) {
        return serverClient.getRandom(new RunForm<>(runToken, new RandomBlockForm(block)));
    }
    public TokenResponse<Timebox> timeboxRun(String hash, String cause) {
        return serverClient.timeboxRun(new RunForm<>(runToken, new TimeboxForm(hash, cause)));
    }
}
