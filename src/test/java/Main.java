import auth.YggdrasilAuthentication;
import auth.YggdrasilAuthenticationFactory;
import auth.YggdrasilKeyPair;
import client.EventTokenLogger;
import client.RandomType;
import client.RefreshTokenStore;
import client.RunManager;
import model.RandomBlock;
import model.RunStart;
import model.TokenResponse;
import run.Run;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

enum RandomTypes implements RandomType {
    FLINT_RATES;

    @Override
    public int sourceIndex() {
        return this.ordinal();
    }
}

class RefreshTokenStoreImpl implements RefreshTokenStore {
    @Override
    public void saveRefreshToken(String refreshToken) {
        System.out.println(refreshToken);
    }

    @Override
    public String getRefreshToken() {
        return "eyJhbGciOiJIUzI1NiJ9.YTIzM2U1ZGItMGI0Yy00NDBhLWIzN2UtMmJjNDNjNGU2MzYx.H5B2pbUO6aIkIe55o54RKDcP4VmY33NHNc8t6na9828";
    }
}

class EventTokenLoggerImpl implements EventTokenLogger {
    @Override
    public void startRun(String runKey, TokenResponse<RunStart> runStart) {
        System.out.println("start run: " + runKey + " " + runStart.token());
    }

    @Override
    public void randomBlock(String runKey, TokenResponse<RandomBlock> randomBlock) {
        System.out.println("random block: " + runKey + " " + randomBlock.token());
    }
}

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        YggdrasilAuthentication yggdrasilAuthentication = new YggdrasilAuthenticationFactory()
            .uuid(
                new UUID(0L, 0L)
            )
            .keyPair(
                new YggdrasilKeyPair(
                    "publicKeySignature",
                    0L,
                        privateKey,
                        publicKey
                )
            ).build();
        RunManager<RandomTypes> runManager = new RunManager<>(new RefreshTokenStoreImpl(), new EventTokenLoggerImpl(), yggdrasilAuthentication);

        Run<RandomTypes> run = runManager.startRun(null, "runKey");
        System.out.println(run.getRandom(RandomTypes.FLINT_RATES).nextInt());
        System.out.println(run.timebox("hash", "cause"));
    }
}
