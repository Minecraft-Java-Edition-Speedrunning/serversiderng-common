package auth;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class YggdrasilAuthenticationFactory {
    UUID uuid;
    YggdrasilKeyPair keyPair;

    @SuppressWarnings("unused")
    public YggdrasilAuthenticationFactory() {}
    private YggdrasilAuthenticationFactory(UUID uuid, YggdrasilKeyPair keyPair) {
        this.uuid = uuid;
        this.keyPair = keyPair;
    }

    @SuppressWarnings("unused")
    public YggdrasilAuthenticationFactory uuid(UUID uuid){
        return new YggdrasilAuthenticationFactory(uuid, this.keyPair);
    }

    @SuppressWarnings("unused")
    public YggdrasilAuthenticationFactory keyPair(YggdrasilKeyPair keyPair){
        return new YggdrasilAuthenticationFactory(this.uuid, keyPair);
    }

    @SuppressWarnings("unused")
    public YggdrasilAuthentication build() {
        if (uuid == null) {
            throw new RuntimeException("uuid not provided to YggdrasilAuthenticationFactory");
        }
        if ( keyPair == null) {
            throw new RuntimeException("keyPair not provided to YggdrasilAuthenticationFactory");
        }

        long challenge = new SecureRandom().nextLong();
        long challengeExpiration = Instant.now().toEpochMilli() + 30 * 1000;

        byte[] challengeBytes = ByteBuffer.allocate(Long.BYTES).putLong(challenge).array();
        byte[] challengeExpirationBytes = ByteBuffer.allocate(Long.BYTES).putLong(challengeExpiration).array();
        String challengeString = Base64.getEncoder().encodeToString(challengeBytes);
        try {
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyPair.privateKey()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(challengeBytes);
            signature.update(challengeExpirationBytes);
            byte[] challengeSignatureBytes = signature.sign();
            String challengeSignature = Base64.getEncoder().encodeToString(challengeSignatureBytes);

            return new YggdrasilAuthentication(
                uuid.toString(),
                keyPair.publicKey(),
                keyPair.expiresAt(),
                keyPair.publicKeySignature(),
                challengeString,
                challengeExpiration,
                challengeSignature
            );
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
