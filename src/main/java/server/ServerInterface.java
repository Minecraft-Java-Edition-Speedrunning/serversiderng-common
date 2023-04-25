package server;

import auth.AccessRefreshToken;
import auth.YggdrasilAuthentication;
import com.google.gson.reflect.TypeToken;
import model.RandomBlock;
import model.RandomBlockForm;
import model.RunForm;
import model.RunStart;
import model.Timebox;
import model.TimeboxForm;
import model.TokenResponse;
import java.io.IOException;

public class ServerInterface {

    private final RequestFactory requestFactory;

    public ServerInterface(String host) {
        this.requestFactory = new RequestFactory().host(host);
    }

    public AccessRefreshToken authenticateYggdrasil(YggdrasilAuthentication authentication) {
        try {
            return requestFactory
                .endpoint("/authentication/yggdrasil")
                .method("POST")
                .body(authentication)
                .request(new TypeToken<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AccessRefreshToken refresh(String refreshToken) {
        try {
            return requestFactory
                .endpoint("/authentication/refresh")
                .method("POST")
                .body(refreshToken)
                .header("Content-Type", "text/plain; charset=utf-8")
                .request(new TypeToken<>() {});
        } catch (IOException e) {
            return null;
        }
    }

    public TokenResponse<RunStart> startRun(
        String authorization,
        String seed
    ) {
        try {
            return requestFactory
                .endpoint("/api/v2/verification/start_run")
                .method("POST")
                .queryParam("seed", seed)
                .header("Authorization", authorization)
                .request(new TypeToken<>() {});
        } catch (IOException e) {
            return null;
        }
    }
    public TokenResponse<RandomBlock> getRandom(
        String authorization,
        RunForm<RandomBlockForm> randomBlockForm
    ) {
        try {
            return requestFactory
                    .endpoint("/api/v2/verification/get_random")
                    .method("POST")
                    .body(randomBlockForm)
                    .header("Authorization", authorization)
                    .request(new TypeToken<>() {});
        } catch (IOException e) {
            return null;
        }
    }
    public TokenResponse<Timebox> timeboxRun(
        String authorization,
        RunForm<TimeboxForm> timeboxForm
    ) {
        try {
            return requestFactory
                    .endpoint("/api/v2/verification/timebox_run")
                    .method("POST")
                    .body(timeboxForm)
                    .header("Authorization", authorization)
                    .request(new TypeToken<>() {});
        } catch (IOException e) {
            return null;
        }
    }
}
