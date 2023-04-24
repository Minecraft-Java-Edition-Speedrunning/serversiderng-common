package server;

import auth.AccessRefreshToken;
import model.RandomBlock;
import model.RunStart;
import model.Timebox;
import model.TokenResponse;

public interface ServerInterface {
    AccessRefreshToken authenticateYggdrasil(
        String uuid,
        String publicKey,
        Long publicKeyExpiration,
        String publicKeySignature,
        String challenge,
        Long challengeExpiration,
        String challengeSignature
    );
    AccessRefreshToken refresh(String refreshToken);
    TokenResponse<RunStart> startRun(
        String authorization,
        String seed
    );
    TokenResponse<RandomBlock> getRandom(
        String authorization,
        String runToken,
        Long block
    );
    TokenResponse<Timebox> timeboxRun(
        String authorization,
        String runToken,
        String hash,
        String cause
    );
}
