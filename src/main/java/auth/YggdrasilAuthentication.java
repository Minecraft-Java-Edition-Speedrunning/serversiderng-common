package auth;

public class YggdrasilAuthentication {
    public final String uuid;
    public final String publicKey;
    public final Long publicKeyExpiration;
    public final String publicKeySignature;
    public final String challenge;
    public final Long challengeExpiration;
    public final String challengeSignature;
    protected YggdrasilAuthentication (
            String uuid,
            String publicKey,
            Long publicKeyExpiration,
            String publicKeySignature,
            String challenge,
            Long challengeExpiration,
            String challengeSignature
    ) {
        this.uuid = uuid;
        this.publicKey = publicKey;
        this.publicKeyExpiration = publicKeyExpiration;
        this.publicKeySignature = publicKeySignature;
        this.challenge = challenge;
        this.challengeExpiration = challengeExpiration;
        this.challengeSignature = challengeSignature;
    }
}
